package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Telemetry payload (entry type 6/7).
 * JSON fields: v, type, source, level, ts, device_id, readings, attributes, meta
 */
public final class PayloadTelemetry {

    private PayloadTelemetry() {}

    /**
     * A single sensor reading.
     */
    public static Map<String, Object> reading(String name, double value, String unit) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", name);
        r.put("value", value);
        if (unit != null && !unit.isEmpty()) {
            r.put("unit", unit);
        }
        return r;
    }

    /**
     * Creates a telemetry payload map.
     */
    public static Map<String, Object> create(String source, String deviceId,
                                              List<Map<String, Object>> readings,
                                              Map<String, String> attributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "telemetry");
        payload.put("source", source != null ? source : "");
        payload.put("level", 7); // info
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));
        payload.put("device_id", deviceId);
        payload.put("readings", readings != null ? new ArrayList<>(readings) : new ArrayList<>());
        if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }
        return payload;
    }
}
