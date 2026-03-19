package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log payload (entry type 1).
 * JSON fields: v, type, source, level, ts, message, attributes, meta
 */
public final class PayloadLog {

    private PayloadLog() {}

    /**
     * Creates a log payload map.
     */
    public static Map<String, Object> create(String source, String message, int level,
                                              Map<String, String> attributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "log");
        payload.put("source", source != null ? source : "");
        payload.put("level", level);
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));
        payload.put("message", message);
        if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }
        return payload;
    }
}
