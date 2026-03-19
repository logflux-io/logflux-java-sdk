package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Event payload (entry type 4).
 * JSON fields: v, type, source, level, ts, event, attributes, meta
 */
public final class PayloadEvent {

    private PayloadEvent() {}

    /**
     * Creates an event payload map.
     */
    public static Map<String, Object> create(String source, String event,
                                              Map<String, String> attributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "event");
        payload.put("source", source != null ? source : "");
        payload.put("level", 7); // info
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));
        payload.put("event", event);
        if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }
        return payload;
    }
}
