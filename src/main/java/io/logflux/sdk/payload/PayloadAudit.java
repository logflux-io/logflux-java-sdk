package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Audit payload (entry type 5, Object Lock).
 * JSON fields: v, type, source, level, ts, action, actor, resource, resource_id,
 *              attributes, meta
 */
public final class PayloadAudit {

    private PayloadAudit() {}

    /**
     * Creates an audit payload map.
     */
    public static Map<String, Object> create(String source, String action, String actor,
                                              String resource, String resourceId,
                                              Map<String, String> attributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "audit");
        payload.put("source", source != null ? source : "");
        payload.put("level", 6); // notice
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));
        payload.put("action", action);
        payload.put("actor", actor);
        payload.put("resource", resource);
        payload.put("resource_id", resourceId);
        if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }
        return payload;
    }
}
