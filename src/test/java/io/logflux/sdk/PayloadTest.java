package io.logflux.sdk;

import io.logflux.sdk.payload.*;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PayloadTest {

    @Test
    void logPayloadHasRequiredFields() {
        Map<String, Object> p = PayloadLog.create("test-source", "hello world", LogLevel.INFO, null);
        assertEquals("2.0", p.get("v"));
        assertEquals("log", p.get("type"));
        assertEquals("test-source", p.get("source"));
        assertEquals(7, p.get("level"));
        assertEquals("hello world", p.get("message"));
        assertNotNull(p.get("ts"));
        assertNull(p.get("attributes"));
    }

    @Test
    void logPayloadWithAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("key1", "value1");
        attrs.put("key2", "value2");
        Map<String, Object> p = PayloadLog.create("src", "msg", LogLevel.ERROR, attrs);
        assertEquals(4, p.get("level"));
        @SuppressWarnings("unchecked")
        Map<String, Object> pAttrs = (Map<String, Object>) p.get("attributes");
        assertNotNull(pAttrs);
        assertEquals("value1", pAttrs.get("key1"));
    }

    @Test
    void metricPayloadFields() {
        Map<String, Object> p = PayloadMetric.create("src", "cpu.usage", 85.5, "gauge", "%", null);
        assertEquals("metric", p.get("type"));
        assertEquals("cpu.usage", p.get("name"));
        assertEquals(85.5, p.get("value"));
        assertEquals("gauge", p.get("metric_type"));
        assertEquals("%", p.get("unit"));
    }

    @Test
    void tracePayloadFields() {
        Instant start = Instant.now().minusSeconds(1);
        Instant end = Instant.now();
        Map<String, Object> p = PayloadTrace.create("src", "trace123", "span456", "parent789",
                "http.server", "GET /api", "ok", start, end, null);
        assertEquals("trace", p.get("type"));
        assertEquals("trace123", p.get("trace_id"));
        assertEquals("span456", p.get("span_id"));
        assertEquals("parent789", p.get("parent_span_id"));
        assertEquals("http.server", p.get("operation"));
        assertEquals("GET /api", p.get("description"));
        assertEquals("ok", p.get("status"));
        assertNotNull(p.get("start_time"));
        assertNotNull(p.get("end_time"));
        assertTrue(((Number) p.get("duration_ms")).longValue() >= 0);
    }

    @Test
    void eventPayloadFields() {
        Map<String, String> attrs = Collections.singletonMap("user", "admin");
        Map<String, Object> p = PayloadEvent.create("src", "user.login", attrs);
        assertEquals("event", p.get("type"));
        assertEquals("user.login", p.get("event"));
        assertNotNull(p.get("attributes"));
    }

    @Test
    void auditPayloadFields() {
        Map<String, Object> p = PayloadAudit.create("src", "delete", "admin@test.com",
                "user", "usr_123", null);
        assertEquals("audit", p.get("type"));
        assertEquals(6, p.get("level")); // notice
        assertEquals("delete", p.get("action"));
        assertEquals("admin@test.com", p.get("actor"));
        assertEquals("user", p.get("resource"));
        assertEquals("usr_123", p.get("resource_id"));
    }

    @Test
    void errorPayloadFields() {
        Exception cause = new RuntimeException("root cause");
        Exception ex = new IllegalStateException("something broke", cause);
        Map<String, Object> p = PayloadError.create("src", ex, null, null, null);
        assertEquals("log", p.get("type"));
        assertEquals(4, p.get("level")); // error
        assertEquals("something broke", p.get("message"));
        assertEquals("java.lang.IllegalStateException", p.get("error_type"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chain = (List<Map<String, Object>>) p.get("error_chain");
        assertNotNull(chain, "should have error chain for wrapped exceptions");
        assertEquals(2, chain.size());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> stack = (List<Map<String, Object>>) p.get("stack_trace");
        assertNotNull(stack);
        assertFalse(stack.isEmpty());
    }

    @Test
    void errorPayloadWithCustomMessage() {
        Exception ex = new RuntimeException("internal error");
        Map<String, Object> p = PayloadError.create("src", ex, "Failed to process request", null, null);
        assertEquals("Failed to process request", p.get("message"));
        @SuppressWarnings("unchecked")
        Map<String, Object> attrs = (Map<String, Object>) p.get("attributes");
        assertNotNull(attrs);
        assertEquals("internal error", attrs.get("error"));
    }

    @Test
    void telemetryPayloadFields() {
        List<Map<String, Object>> readings = new ArrayList<>();
        readings.add(PayloadTelemetry.reading("temperature", 22.5, "celsius"));
        readings.add(PayloadTelemetry.reading("humidity", 65.0, "%"));

        Map<String, Object> p = PayloadTelemetry.create("src", "device-001", readings, null);
        assertEquals("telemetry", p.get("type"));
        assertEquals("device-001", p.get("device_id"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> r = (List<Map<String, Object>>) p.get("readings");
        assertEquals(2, r.size());
        assertEquals("temperature", r.get(0).get("name"));
        assertEquals(22.5, r.get(0).get("value"));
    }

    @Test
    void jsonSerializationRoundTrip() {
        Map<String, Object> p = PayloadLog.create("src", "test message", LogLevel.INFO, null);
        String json = Json.toJson(p);
        assertTrue(json.contains("\"v\":\"2.0\""));
        assertTrue(json.contains("\"type\":\"log\""));
        assertTrue(json.contains("\"message\":\"test message\""));
        assertTrue(json.contains("\"source\":\"src\""));
        assertTrue(json.contains("\"level\":7"));
    }

    @Test
    void jsonEscapesSpecialCharacters() {
        Map<String, String> attrs = Collections.singletonMap("msg", "line1\nline2\ttab\"quote\\back");
        Map<String, Object> p = PayloadLog.create("src", "test", LogLevel.INFO, attrs);
        String json = Json.toJson(p);
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\\\"));
    }
}
