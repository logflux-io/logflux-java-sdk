package io.logflux.sdk;

import io.logflux.sdk.payload.PayloadContext;
import io.logflux.sdk.payload.PayloadTrace;
import io.logflux.sdk.payload.Json;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an in-flight trace span. Call end() to finish and send it.
 *
 * Usage:
 *   Span span = LogFlux.startSpan("http.server", "GET /api/users");
 *   try {
 *       // ... do work ...
 *       span.setAttribute("http.status_code", "200");
 *   } catch (Exception e) {
 *       span.setError(e);
 *       throw e;
 *   } finally {
 *       span.end();
 *   }
 */
public final class Span {

    /** Trace propagation header name. */
    public static final String TRACE_HEADER = "X-LogFlux-Trace";

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String operation;
    private final String description;
    private final Instant startTime;
    private String status;
    private Map<String, String> attributes;
    private volatile boolean ended;
    private final Object lock = new Object();

    Span(String traceId, String spanId, String parentSpanId,
         String operation, String description) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.operation = operation;
        this.description = description;
        this.startTime = Instant.now();
        this.status = "ok";
    }

    /**
     * Creates a new root span (generates new trace ID and span ID).
     */
    public static Span create(String operation, String description) {
        return new Span(generateTraceId(), generateSpanId(), null,
                operation, description);
    }

    /**
     * Creates a child span under this span (same trace ID).
     */
    public Span startChild(String operation, String description) {
        return new Span(this.traceId, generateSpanId(), this.spanId,
                operation, description);
    }

    /**
     * Creates a span that continues from trace headers.
     * If the header is missing or invalid, starts a new root span.
     */
    public static Span continueFromHeaders(Map<String, String> headers,
                                            String operation, String description) {
        if (headers == null) return create(operation, description);
        String header = headers.get(TRACE_HEADER);
        if (header == null || header.isEmpty()) return create(operation, description);

        String[] parts = header.split("-", 3);
        if (parts.length < 2) return create(operation, description);

        String traceId = parts[0];
        String parentSpanId = parts[1];

        // Validate
        if (traceId.length() != 32 || parentSpanId.length() != 16) {
            return create(operation, description);
        }
        if (!isHex(traceId) || !isHex(parentSpanId)) {
            return create(operation, description);
        }

        return new Span(traceId, generateSpanId(), parentSpanId,
                operation, description);
    }

    /**
     * Ends the span, computes duration, and sends it as a trace entry.
     */
    public void end() {
        synchronized (lock) {
            if (ended) return;
            ended = true;
        }

        Client client = LogFlux.getClientInternal();
        if (client == null) return;

        Instant endTime = Instant.now();
        Map<String, String> attrsCopy;
        String statusCopy;
        synchronized (lock) {
            attrsCopy = attributes != null ? new HashMap<>(attributes) : null;
            statusCopy = status;
        }

        Map<String, Object> payload = PayloadTrace.create("", traceId, spanId,
                parentSpanId, operation, description, statusCopy,
                startTime, endTime, attrsCopy);
        PayloadContext.apply(payload);

        // Apply beforeSendTrace hook
        payload = client.applyHook(payload, "trace");
        if (payload == null) return;

        String json = Json.toJson(payload);
        client.enqueue(json, LogLevel.INFO, EntryType.TRACE);
    }

    // --- Setters ---

    public void setAttribute(String key, String value) {
        synchronized (lock) {
            if (attributes == null) attributes = new HashMap<>();
            attributes.put(key, value);
        }
    }

    public void setAttributes(Map<String, String> attrs) {
        if (attrs == null) return;
        synchronized (lock) {
            if (attributes == null) attributes = new HashMap<>();
            attributes.putAll(attrs);
        }
    }

    public void setStatus(String status) {
        synchronized (lock) {
            this.status = status;
        }
    }

    public void setError(Throwable error) {
        if (error == null) return;
        synchronized (lock) {
            this.status = "error";
            if (attributes == null) attributes = new HashMap<>();
            attributes.put("error.message", error.getMessage() != null
                    ? error.getMessage() : error.getClass().getName());
        }
    }

    // --- Getters ---

    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }

    /**
     * Returns the trace header value for propagation.
     * Format: traceId-spanId-sampled
     */
    public String toTraceHeader() {
        return traceId + "-" + spanId + "-1";
    }

    /**
     * Injects trace context into a map of headers for outgoing requests.
     */
    public void injectHeaders(Map<String, String> headers) {
        if (headers != null) {
            headers.put(TRACE_HEADER, toTraceHeader());
        }
    }

    // --- ID generation ---

    static String generateTraceId() {
        byte[] b = new byte[16]; // 32 hex chars
        RANDOM.nextBytes(b);
        return bytesToHex(b);
    }

    static String generateSpanId() {
        byte[] b = new byte[8]; // 16 hex chars
        RANDOM.nextBytes(b);
        return bytesToHex(b);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
