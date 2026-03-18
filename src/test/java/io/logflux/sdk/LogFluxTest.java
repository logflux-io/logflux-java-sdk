package io.logflux.sdk;

import io.logflux.sdk.payload.Json;
import io.logflux.sdk.transport.Discovery;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogFluxTest {

    @Test
    void logMethodsAreNoOpWithoutInit() {
        // All methods should silently do nothing when SDK is not initialized
        assertDoesNotThrow(() -> LogFlux.debug("test"));
        assertDoesNotThrow(() -> LogFlux.info("test"));
        assertDoesNotThrow(() -> LogFlux.notice("test"));
        assertDoesNotThrow(() -> LogFlux.warn("test"));
        assertDoesNotThrow(() -> LogFlux.error("test"));
        assertDoesNotThrow(() -> LogFlux.critical("test"));
        assertDoesNotThrow(() -> LogFlux.alert("test"));
        assertDoesNotThrow(() -> LogFlux.emergency("test"));
    }

    @Test
    void metricMethodsAreNoOpWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.counter("test", 1.0, null));
        assertDoesNotThrow(() -> LogFlux.gauge("test", 42.0, null));
        assertDoesNotThrow(() -> LogFlux.metric("test", 1.0, "counter", null));
    }

    @Test
    void eventIsNoOpWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.event("test.event", null));
    }

    @Test
    void auditIsNoOpWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.audit("create", "admin", "user", "usr_1", null));
    }

    @Test
    void captureErrorIsNoOpWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.captureError(new RuntimeException("test")));
        assertDoesNotThrow(() -> LogFlux.captureError(null));
        assertDoesNotThrow(() -> LogFlux.captureErrorWithMessage(
                new RuntimeException("test"), "custom msg", null));
    }

    @Test
    void breadcrumbsWorkWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.addBreadcrumb("http", "GET /api", null));
        assertDoesNotThrow(LogFlux::clearBreadcrumbs);
    }

    @Test
    void withScopeWorksWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.withScope(scope -> {
            scope.setAttribute("key", "value");
            scope.info("scoped message");
            scope.captureError(new RuntimeException("scoped error"));
        }));
    }

    @Test
    void startSpanWorksWithoutInit() {
        Span span = LogFlux.startSpan("test.op", "description");
        assertNotNull(span);
        assertNotNull(span.getTraceId());
        span.end(); // should be no-op without client
    }

    @Test
    void continueFromHeadersWorksWithoutInit() {
        Span span = LogFlux.continueFromHeaders(null, "test.op", "desc");
        assertNotNull(span);
    }

    @Test
    void statsReturnsEmptyWithoutInit() {
        ClientStats stats = LogFlux.stats();
        assertNotNull(stats);
        assertEquals(0, stats.getEntriesSent());
        assertEquals(0, stats.getEntriesDropped());
    }

    @Test
    void isActiveReturnsFalseWithoutInit() {
        assertFalse(LogFlux.isActive());
    }

    @Test
    void closeIsIdempotent() {
        assertDoesNotThrow(LogFlux::close);
        assertDoesNotThrow(LogFlux::close);
    }

    @Test
    void flushIsNoOpWithoutInit() {
        assertDoesNotThrow(() -> LogFlux.flush(1000));
    }

    @Test
    void versionIsSet() {
        assertEquals("3.0.0", Version.VERSION);
        assertTrue(Version.USER_AGENT.startsWith("logflux-java-sdk/"));
    }

    @Test
    void entryTypeConstants() {
        assertEquals(1, EntryType.LOG);
        assertEquals(2, EntryType.METRIC);
        assertEquals(3, EntryType.TRACE);
        assertEquals(4, EntryType.EVENT);
        assertEquals(5, EntryType.AUDIT);
        assertEquals(6, EntryType.TELEMETRY);
        assertEquals(7, EntryType.TELEMETRY_MANAGED);
    }

    @Test
    void entryTypeValidation() {
        assertTrue(EntryType.isValid(1));
        assertTrue(EntryType.isValid(7));
        assertFalse(EntryType.isValid(0));
        assertFalse(EntryType.isValid(8));
    }

    @Test
    void entryTypeRequiresEncryption() {
        assertTrue(EntryType.requiresEncryption(1));
        assertTrue(EntryType.requiresEncryption(6));
        assertFalse(EntryType.requiresEncryption(7));
    }

    @Test
    void entryTypeCategories() {
        assertEquals("events", EntryType.category(EntryType.LOG));
        assertEquals("events", EntryType.category(EntryType.METRIC));
        assertEquals("events", EntryType.category(EntryType.EVENT));
        assertEquals("traces", EntryType.category(EntryType.TRACE));
        assertEquals("traces", EntryType.category(EntryType.TELEMETRY));
        assertEquals("traces", EntryType.category(EntryType.TELEMETRY_MANAGED));
        assertEquals("audit", EntryType.category(EntryType.AUDIT));
    }

    @Test
    void logLevelConstants() {
        assertEquals(1, LogLevel.EMERGENCY);
        assertEquals(2, LogLevel.ALERT);
        assertEquals(3, LogLevel.CRITICAL);
        assertEquals(4, LogLevel.ERROR);
        assertEquals(5, LogLevel.WARNING);
        assertEquals(6, LogLevel.NOTICE);
        assertEquals(7, LogLevel.INFO);
        assertEquals(8, LogLevel.DEBUG);
    }

    @Test
    void logLevelValidation() {
        assertTrue(LogLevel.isValid(1));
        assertTrue(LogLevel.isValid(8));
        assertFalse(LogLevel.isValid(0));
        assertFalse(LogLevel.isValid(9));
    }

    @Test
    void logLevelToString() {
        assertEquals("emergency", LogLevel.toString(1));
        assertEquals("debug", LogLevel.toString(8));
    }

    @Test
    void jsonParsing() {
        String json = "{\"status\":\"ok\",\"key_uuid\":\"abc-123\",\"count\":42,\"active\":true}";
        java.util.Map<String, Object> parsed = Json.parseObject(json);
        assertNotNull(parsed);
        assertEquals("ok", Json.getString(parsed, "status"));
        assertEquals("abc-123", Json.getString(parsed, "key_uuid"));
        assertEquals(42, Json.getInt(parsed, "count", 0));
        assertTrue(Json.getBoolean(parsed, "active", false));
    }

    @Test
    void jsonParsingNested() {
        String json = "{\"data\":{\"public_key\":\"PEM-DATA\",\"supports_multipart\":true},\"status\":\"ok\"}";
        java.util.Map<String, Object> parsed = Json.parseObject(json);
        java.util.Map<String, Object> data = Json.getObject(parsed, "data");
        assertNotNull(data);
        assertEquals("PEM-DATA", Json.getString(data, "public_key"));
        assertTrue(Json.getBoolean(data, "supports_multipart", false));
    }

    @Test
    void discoveryExtractRegion() {
        assertEquals("eu", Discovery.extractRegion("eu-lf_test123"));
        assertEquals("us", Discovery.extractRegion("us-lf_test123"));
        assertEquals("ca", Discovery.extractRegion("ca-lf_test123"));
        assertEquals("au", Discovery.extractRegion("au-lf_test123"));
        assertEquals("ap", Discovery.extractRegion("ap-lf_test123"));
        assertNull(Discovery.extractRegion("xx-lf_test123"));
        assertNull(Discovery.extractRegion("no-prefix"));
        assertNull(Discovery.extractRegion(null));
    }
}
