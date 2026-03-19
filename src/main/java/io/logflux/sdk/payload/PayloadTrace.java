package io.logflux.sdk.payload;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trace payload (entry type 3).
 * JSON fields: v, type, source, level, ts, trace_id, span_id, parent_span_id,
 *              operation, description, status, start_time, end_time, duration_ms,
 *              attributes, meta
 */
public final class PayloadTrace {

    private PayloadTrace() {}

    /**
     * Creates a trace payload map.
     */
    public static Map<String, Object> create(String source, String traceId, String spanId,
                                              String parentSpanId, String operation,
                                              String description, String status,
                                              Instant startTime, Instant endTime,
                                              Map<String, String> attributes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("v", "2.0");
        payload.put("type", "trace");
        payload.put("source", source != null ? source : "");
        payload.put("level", 7); // info
        payload.put("ts", Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT));
        payload.put("trace_id", traceId);
        payload.put("span_id", spanId);
        if (parentSpanId != null && !parentSpanId.isEmpty()) {
            payload.put("parent_span_id", parentSpanId);
        }
        payload.put("operation", operation);
        if (description != null && !description.isEmpty()) {
            payload.put("description", description);
        }
        payload.put("status", status != null ? status : "ok");

        DateTimeFormatter fmt = DateTimeFormatter.ISO_INSTANT;
        payload.put("start_time", startTime.atOffset(ZoneOffset.UTC).format(fmt));
        payload.put("end_time", endTime.atOffset(ZoneOffset.UTC).format(fmt));

        long durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();
        if (durationMs >= 0) {
            payload.put("duration_ms", durationMs);
        }
        if (attributes != null && !attributes.isEmpty()) {
            payload.put("attributes", new LinkedHashMap<>(attributes));
        }
        return payload;
    }
}
