package io.logflux.sdk;

import io.logflux.sdk.payload.PayloadContext;
import io.logflux.sdk.payload.PayloadEvent;
import io.logflux.sdk.payload.PayloadLog;
import io.logflux.sdk.payload.PayloadError;
import io.logflux.sdk.payload.Json;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-request context isolation. Attributes and breadcrumbs set on a scope
 * are merged into every entry sent through it, without affecting other scopes
 * or the global state.
 *
 * Usage:
 *   LogFlux.withScope(scope -> {
 *       scope.setAttribute("request_id", "abc-123");
 *       scope.addBreadcrumb("http", "GET /api/users", null);
 *       scope.info("processing request");
 *       scope.captureError(exception);
 *   });
 */
public final class Scope {

    private final Map<String, String> attributes = new HashMap<>();
    private final Breadcrumb.Ring breadcrumbs;
    private String traceId;
    private String spanId;

    Scope(int maxBreadcrumbs) {
        this.breadcrumbs = new Breadcrumb.Ring(maxBreadcrumbs > 0 ? maxBreadcrumbs : 100);
    }

    // --- Attribute setters ---

    public synchronized void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public synchronized void setAttributes(Map<String, String> attrs) {
        if (attrs != null) attributes.putAll(attrs);
    }

    public synchronized void setUser(String userId) {
        attributes.put("user.id", userId);
    }

    public synchronized void setRequest(String method, String path, String requestId) {
        attributes.put("http.method", method);
        attributes.put("http.path", path);
        if (requestId != null && !requestId.isEmpty()) {
            attributes.put("request_id", requestId);
        }
    }

    // --- Trace context ---

    public void setTraceContext(String traceId, String spanId) {
        this.traceId = traceId;
        this.spanId = spanId;
    }

    // --- Breadcrumbs ---

    public void addBreadcrumb(String category, String message, Map<String, String> data) {
        breadcrumbs.add(new Breadcrumb(category, message, data));
    }

    // --- Log methods ---

    public void debug(String message) { log(LogLevel.DEBUG, message); }
    public void info(String message)  { log(LogLevel.INFO, message); }
    public void notice(String message) { log(LogLevel.NOTICE, message); }
    public void warn(String message)  { log(LogLevel.WARNING, message); }
    public void error(String message) { log(LogLevel.ERROR, message); }
    public void critical(String message) { log(LogLevel.CRITICAL, message); }

    /**
     * Sends a log entry with scope attributes merged in.
     */
    public void log(int level, String message) {
        Client client = LogFlux.getClientInternal();
        if (client == null) return;

        Map<String, Object> payload = PayloadLog.create("", message, level, null);
        PayloadContext.apply(payload);
        applyScope(payload);

        if (level <= LogLevel.INFO) {
            breadcrumbs.add(new Breadcrumb("log", message,
                    LogLevel.toCategory(level), null));
        }

        String json = Json.toJson(payload);
        client.enqueue(json, level, EntryType.LOG);
    }

    /**
     * Captures an error with scope context and breadcrumbs.
     */
    public void captureError(Throwable error) {
        captureError(error, null);
    }

    public void captureError(Throwable error, Map<String, String> attrs) {
        if (error == null) return;
        Client client = LogFlux.getClientInternal();
        if (client == null) return;

        Map<String, Object> payload = PayloadError.create("", error, null, attrs,
                breadcrumbs.snapshot());
        PayloadContext.apply(payload);
        applyScope(payload);

        String json = Json.toJson(payload);
        client.enqueue(json, LogLevel.ERROR, EntryType.LOG);
    }

    /**
     * Sends an event with scope attributes.
     */
    public void event(String event, Map<String, String> attrs) {
        Client client = LogFlux.getClientInternal();
        if (client == null) return;

        Map<String, Object> payload = PayloadEvent.create("", event, attrs);
        PayloadContext.apply(payload);
        applyScope(payload);

        breadcrumbs.add(new Breadcrumb("event", event, attrs));

        String json = Json.toJson(payload);
        client.enqueue(json, LogLevel.INFO, EntryType.EVENT);
    }

    /**
     * Merges scope attributes into a payload.
     * Scope attributes are defaults -- they don't overwrite existing ones.
     */
    @SuppressWarnings("unchecked")
    private synchronized void applyScope(Map<String, Object> payload) {
        if (attributes.isEmpty()) return;
        Map<String, Object> existing = (Map<String, Object>) payload.get("attributes");
        if (existing == null) {
            existing = new HashMap<>();
            payload.put("attributes", existing);
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!existing.containsKey(entry.getKey())) {
                existing.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
