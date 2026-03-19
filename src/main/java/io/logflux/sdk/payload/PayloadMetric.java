package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Metric payload (entry type 2).
 * JSON fields: v, type, source, level, ts, name, value, metric_type, unit, attributes, meta
 */
public final class PayloadMetric {

    private PayloadMetric() {}

    /**
     * Creates a metric payload map.
     */
    public static Map<String, Object> create(String source, String name, double value,
                                              String metricType, String unit,
                                              Map<String, String> attributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "metric");
        payload.put("source", source != null ? source : "");
        payload.put("level", 7); // info
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));
        payload.put("name", name);
        payload.put("value", value);
        if (metricType != null && !metricType.isEmpty()) {
            payload.put("metric_type", metricType);
        }
        if (unit != null && !unit.isEmpty()) {
            payload.put("unit", unit);
        }
        if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }
        return payload;
    }
}
