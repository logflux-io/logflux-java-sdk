package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Error payload (entry type 1 with error fields).
 * JSON fields: v, type, source, level, ts, message, error_type, error_message,
 *              stack_trace, error_chain, breadcrumbs, attributes, meta
 */
public final class PayloadError {

    private PayloadError() {}

    /**
     * Creates an error payload map from a Throwable.
     */
    public static Map<String, Object> create(String source, Throwable error,
                                              String customMessage,
                                              Map<String, String> attributes,
                                              List<Map<String, Object>> breadcrumbs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "log");
        payload.put("source", source != null ? source : "");
        payload.put("level", 4); // error
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));

        // Message: custom message or error message
        String message = customMessage != null ? customMessage : error.getMessage();
        if (message == null) message = error.getClass().getName();
        payload.put("message", message);

        // Error type
        payload.put("error_type", error.getClass().getName());
        payload.put("error_message", error.getMessage() != null ? error.getMessage() : "");

        // Stack trace
        List<Map<String, Object>> stackFrames = captureStackTrace(error);
        if (!stackFrames.isEmpty()) {
            payload.put("stack_trace", stackFrames);
        }

        // Error chain (follows getCause() up to 10 levels)
        List<Map<String, Object>> chain = unwrapErrorChain(error);
        if (chain != null && chain.size() > 1) {
            payload.put("error_chain", chain);
        }

        // Breadcrumbs
        if (breadcrumbs != null && !breadcrumbs.isEmpty()) {
            payload.put("breadcrumbs", breadcrumbs);
        }

        // Custom message: store original error in attributes
        if (customMessage != null) {
            Map<String, Object> attrs = attributes != null
                    ? new LinkedHashMap<>(attributes) : new LinkedHashMap<>();
            attrs.put("error", error.getMessage() != null ? error.getMessage() : error.getClass().getName());
            payload.put("attributes", attrs);
        } else if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }

        return payload;
    }

    private static List<Map<String, Object>> captureStackTrace(Throwable error) {
        List<Map<String, Object>> frames = new ArrayList<>();
        StackTraceElement[] elements = error.getStackTrace();
        int limit = Math.min(elements.length, 20);
        for (int i = 0; i < limit; i++) {
            StackTraceElement e = elements[i];
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("function", e.getClassName() + "." + e.getMethodName());
            frame.put("file", e.getFileName() != null ? e.getFileName() : "unknown");
            frame.put("line", e.getLineNumber());
            frames.add(frame);
        }
        return frames;
    }

    private static List<Map<String, Object>> unwrapErrorChain(Throwable error) {
        List<Map<String, Object>> chain = new ArrayList<>();
        Throwable current = error;
        for (int i = 0; i < 10 && current != null; i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", current.getClass().getName());
            entry.put("message", current.getMessage() != null ? current.getMessage() : "");
            chain.add(entry);
            current = current.getCause();
        }
        // Only include chain if more than one error
        if (chain.size() <= 1) return null;
        return chain;
    }
}
