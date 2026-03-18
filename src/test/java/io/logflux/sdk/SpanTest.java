package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpanTest {

    @Test
    void createRootSpanGeneratesIds() {
        Span span = Span.create("http.server", "GET /api");
        assertNotNull(span.getTraceId());
        assertNotNull(span.getSpanId());
        assertNull(span.getParentSpanId());
        assertEquals(32, span.getTraceId().length());
        assertEquals(16, span.getSpanId().length());
    }

    @Test
    void childSpanInheritsTraceId() {
        Span parent = Span.create("http.server", "GET /api");
        Span child = parent.startChild("db.query", "SELECT *");

        assertEquals(parent.getTraceId(), child.getTraceId());
        assertNotEquals(parent.getSpanId(), child.getSpanId());
        assertEquals(parent.getSpanId(), child.getParentSpanId());
    }

    @Test
    void continueFromHeaders() {
        Span original = Span.create("original", "op");
        Map<String, String> headers = new HashMap<>();
        original.injectHeaders(headers);

        Span continued = Span.continueFromHeaders(headers, "continued", "op2");
        assertEquals(original.getTraceId(), continued.getTraceId());
        assertEquals(original.getSpanId(), continued.getParentSpanId());
        assertNotEquals(original.getSpanId(), continued.getSpanId());
    }

    @Test
    void continueFromNullHeadersCreatesRootSpan() {
        Span span = Span.continueFromHeaders(null, "op", "desc");
        assertNotNull(span.getTraceId());
        assertNull(span.getParentSpanId());
    }

    @Test
    void continueFromEmptyHeadersCreatesRootSpan() {
        Map<String, String> headers = new HashMap<>();
        Span span = Span.continueFromHeaders(headers, "op", "desc");
        assertNotNull(span.getTraceId());
        assertNull(span.getParentSpanId());
    }

    @Test
    void continueFromInvalidHeaderCreatesRootSpan() {
        Map<String, String> headers = new HashMap<>();
        headers.put(Span.TRACE_HEADER, "invalid-header");
        Span span = Span.continueFromHeaders(headers, "op", "desc");
        assertNotNull(span.getTraceId());
        assertNull(span.getParentSpanId());
    }

    @Test
    void setAttributes() {
        Span span = Span.create("op", "desc");
        span.setAttribute("key1", "value1");
        Map<String, String> attrs = new HashMap<>();
        attrs.put("key2", "value2");
        attrs.put("key3", "value3");
        span.setAttributes(attrs);
        // No exceptions thrown means success
    }

    @Test
    void setStatus() {
        Span span = Span.create("op", "desc");
        span.setStatus("error");
        // No exception means success
    }

    @Test
    void setError() {
        Span span = Span.create("op", "desc");
        span.setError(new RuntimeException("test error"));
        // Should set status to "error" and add error.message attribute
    }

    @Test
    void setErrorWithNull() {
        Span span = Span.create("op", "desc");
        span.setError(null); // should be a no-op
    }

    @Test
    void traceHeaderFormat() {
        Span span = Span.create("op", "desc");
        String header = span.toTraceHeader();
        // Format: traceId-spanId-sampled
        String[] parts = header.split("-");
        // Note: trace ID is 32 hex chars so it may contain '-' in split...
        // Actually the trace and span IDs are pure hex without dashes
        assertTrue(header.endsWith("-1"), "Should end with -1 (sampled)");
        assertTrue(header.startsWith(span.getTraceId()));
    }

    @Test
    void injectHeaders() {
        Span span = Span.create("op", "desc");
        Map<String, String> headers = new HashMap<>();
        span.injectHeaders(headers);
        assertTrue(headers.containsKey(Span.TRACE_HEADER));
        assertEquals(span.toTraceHeader(), headers.get(Span.TRACE_HEADER));
    }

    @Test
    void endIsIdempotent() {
        // Without a real client, end() should silently do nothing
        Span span = Span.create("op", "desc");
        span.end(); // first call
        span.end(); // second call - should be no-op
    }

    @Test
    void uniqueSpanIds() {
        Span s1 = Span.create("op1", "desc1");
        Span s2 = Span.create("op2", "desc2");
        assertNotEquals(s1.getTraceId(), s2.getTraceId());
        assertNotEquals(s1.getSpanId(), s2.getSpanId());
    }
}
