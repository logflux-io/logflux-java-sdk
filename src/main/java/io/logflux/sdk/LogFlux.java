package io.logflux.sdk;

import io.logflux.sdk.payload.Json;
import io.logflux.sdk.payload.PayloadAudit;
import io.logflux.sdk.payload.PayloadContext;
import io.logflux.sdk.payload.PayloadError;
import io.logflux.sdk.payload.PayloadEvent;
import io.logflux.sdk.payload.PayloadLog;
import io.logflux.sdk.payload.PayloadMetric;
import io.logflux.sdk.payload.PayloadTelemetry;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Public facade for the LogFlux SDK.
 * All methods are static and thread-safe. The SDK must never crash the host application.
 *
 * Usage:
 *   LogFlux.init(Options.builder("eu-lf_your_key")
 *       .source("my-service")
 *       .environment("production")
 *       .build());
 *
 *   LogFlux.info("Server started");
 *   LogFlux.counter("requests", 1, null);
 *
 *   // On shutdown:
 *   LogFlux.close();
 */
public final class LogFlux {

    private static volatile Client client;
    private static volatile Breadcrumb.Ring breadcrumbs;
    private static volatile Options currentOptions;
    private static final Object initLock = new Object();

    private LogFlux() {}

    // --- Initialization ---

    /**
     * Initializes the SDK with the given options.
     * Performs endpoint discovery and handshake. May throw on failure.
     */
    public static void init(Options options) throws Exception {
        synchronized (initLock) {
            if (client != null) {
                client.close();
            }

            // Configure global context
            String source = options.source;
            if ((source == null || source.isEmpty()) && options.node != null) {
                source = options.node;
            }
            PayloadContext.configure(source, options.environment, options.release);

            // Initialize breadcrumb ring buffer
            int maxCrumbs = options.maxBreadcrumbs > 0 ? options.maxBreadcrumbs : 100;
            breadcrumbs = new Breadcrumb.Ring(maxCrumbs);

            currentOptions = options;
            client = new Client(options);
        }
    }

    /**
     * Initializes the SDK from environment variables.
     */
    public static void initFromEnv() throws Exception {
        init(Options.fromEnvironment());
    }

    /**
     * Initializes the SDK from environment variables with a specific node name.
     */
    public static void initFromEnv(String node) throws Exception {
        init(Options.fromEnvironment(node));
    }

    /**
     * Closes the SDK: flushes pending entries and zeros key material.
     */
    public static void close() {
        synchronized (initLock) {
            Client c = client;
            if (c != null) {
                c.close();
                client = null;
            }
            PayloadContext.reset();
        }
    }

    /**
     * Flushes pending entries with a timeout.
     */
    public static void flush(long timeoutMs) {
        Client c = client;
        if (c != null) c.flush(timeoutMs);
    }

    // --- Log methods (Type 1) ---

    public static void debug(String message) { log(LogLevel.DEBUG, message, null); }
    public static void debug(String message, Map<String, String> attributes) { log(LogLevel.DEBUG, message, attributes); }

    public static void info(String message) { log(LogLevel.INFO, message, null); }
    public static void info(String message, Map<String, String> attributes) { log(LogLevel.INFO, message, attributes); }

    public static void notice(String message) { log(LogLevel.NOTICE, message, null); }
    public static void notice(String message, Map<String, String> attributes) { log(LogLevel.NOTICE, message, attributes); }

    public static void warn(String message) { log(LogLevel.WARNING, message, null); }
    public static void warn(String message, Map<String, String> attributes) { log(LogLevel.WARNING, message, attributes); }

    public static void error(String message) { log(LogLevel.ERROR, message, null); }
    public static void error(String message, Map<String, String> attributes) { log(LogLevel.ERROR, message, attributes); }

    public static void critical(String message) { log(LogLevel.CRITICAL, message, null); }
    public static void critical(String message, Map<String, String> attributes) { log(LogLevel.CRITICAL, message, attributes); }

    public static void alert(String message) { log(LogLevel.ALERT, message, null); }
    public static void alert(String message, Map<String, String> attributes) { log(LogLevel.ALERT, message, attributes); }

    public static void emergency(String message) { log(LogLevel.EMERGENCY, message, null); }
    public static void emergency(String message, Map<String, String> attributes) { log(LogLevel.EMERGENCY, message, attributes); }

    /**
     * Sends a fatal log message and then calls System.exit(1).
     */
    public static void fatal(String message) { fatal(message, null); }
    public static void fatal(String message, Map<String, String> attributes) {
        log(LogLevel.CRITICAL, message, attributes);
        flush(5000);
        System.exit(1);
    }

    /**
     * Sends a log entry with the given level and attributes.
     */
    public static void log(int level, String message, Map<String, String> attributes) {
        Client c = client;
        if (c == null) return;
        if (!c.shouldSample(EntryType.LOG)) return;

        Map<String, Object> payload = PayloadLog.create("", message, level, attributes);
        PayloadContext.apply(payload);

        // Apply hook
        payload = c.applyHook(payload, "log");
        if (payload == null) return;

        // Add breadcrumb for non-debug levels
        Breadcrumb.Ring b = breadcrumbs;
        if (b != null && level <= LogLevel.INFO) {
            b.add(new Breadcrumb("log", message, LogLevel.toCategory(level), null));
        }

        String json = Json.toJson(payload);
        c.enqueue(json, level, EntryType.LOG);
    }

    // --- Metric methods (Type 2) ---

    public static void counter(String name, double value, Map<String, String> attributes) {
        metric(name, value, "counter", attributes);
    }

    public static void gauge(String name, double value, Map<String, String> attributes) {
        metric(name, value, "gauge", attributes);
    }

    public static void metric(String name, double value, String metricType,
                               Map<String, String> attributes) {
        Client c = client;
        if (c == null) return;
        if (!c.shouldSample(EntryType.METRIC)) return;

        Map<String, Object> payload = PayloadMetric.create("", name, value, metricType, null, attributes);
        PayloadContext.apply(payload);

        payload = c.applyHook(payload, "metric");
        if (payload == null) return;

        String json = Json.toJson(payload);
        c.enqueue(json, LogLevel.INFO, EntryType.METRIC);
    }

    // --- Event methods (Type 4) ---

    public static void event(String name, Map<String, String> attributes) {
        Client c = client;
        if (c == null) return;
        if (!c.shouldSample(EntryType.EVENT)) return;

        Map<String, Object> payload = PayloadEvent.create("", name, attributes);
        PayloadContext.apply(payload);

        payload = c.applyHook(payload, "event");
        if (payload == null) return;

        // Add breadcrumb
        Breadcrumb.Ring b = breadcrumbs;
        if (b != null) {
            b.add(new Breadcrumb("event", name, attributes));
        }

        String json = Json.toJson(payload);
        c.enqueue(json, LogLevel.INFO, EntryType.EVENT);
    }

    // --- Audit methods (Type 5) ---

    /**
     * Sends an audit entry. Audit entries are never sampled (compliance requirement).
     */
    public static void audit(String action, String actor, String resource,
                              String resourceId, Map<String, String> attributes) {
        Client c = client;
        if (c == null) return;
        // Audit entries are never sampled

        Map<String, Object> payload = PayloadAudit.create("", action, actor, resource,
                resourceId, attributes);
        PayloadContext.apply(payload);

        payload = c.applyHook(payload, "audit");
        if (payload == null) return;

        String json = Json.toJson(payload);
        c.enqueue(json, LogLevel.NOTICE, EntryType.AUDIT);
    }

    // --- Error capture ---

    public static void captureError(Throwable error) {
        captureError(error, null);
    }

    public static void captureError(Throwable error, Map<String, String> attributes) {
        captureErrorWithMessage(error, null, attributes);
    }

    public static void captureErrorWithMessage(Throwable error, String message,
                                                Map<String, String> attributes) {
        if (error == null) return;
        Client c = client;
        if (c == null) return;
        if (!c.shouldSample(EntryType.LOG)) return;

        Breadcrumb.Ring b = breadcrumbs;
        List<Map<String, Object>> crumbs = (b != null) ? b.snapshot() : null;

        Map<String, Object> payload = PayloadError.create("", error, message, attributes, crumbs);
        PayloadContext.apply(payload);

        payload = c.applyHook(payload, "error");
        if (payload == null) return;

        String json = Json.toJson(payload);
        c.enqueue(json, LogLevel.ERROR, EntryType.LOG);
    }

    // --- Breadcrumbs ---

    public static void addBreadcrumb(String category, String message, Map<String, String> data) {
        Breadcrumb.Ring b = breadcrumbs;
        if (b == null) return;
        b.add(new Breadcrumb(category, message, data));
    }

    public static void clearBreadcrumbs() {
        Breadcrumb.Ring b = breadcrumbs;
        if (b != null) b.clear();
    }

    // --- Scopes ---

    public static void withScope(Consumer<Scope> callback) {
        Options opts = currentOptions;
        int maxCrumbs = (opts != null) ? opts.maxBreadcrumbs : 100;
        Scope scope = new Scope(maxCrumbs > 0 ? maxCrumbs : 100);
        callback.accept(scope);
    }

    // --- Tracing ---

    public static Span startSpan(String operation, String description) {
        return Span.create(operation, description);
    }

    public static Span continueFromHeaders(Map<String, String> headers,
                                            String operation, String description) {
        return Span.continueFromHeaders(headers, operation, description);
    }

    // --- Stats ---

    public static ClientStats stats() {
        Client c = client;
        if (c == null) return ClientStats.empty();
        return c.getStats();
    }

    public static boolean isActive() {
        Client c = client;
        return c != null && c.isActive();
    }

    // --- Internal (package-private) ---

    static Client getClientInternal() {
        return client;
    }
}
