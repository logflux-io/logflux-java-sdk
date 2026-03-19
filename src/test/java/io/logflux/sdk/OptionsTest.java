package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OptionsTest {

    @Test
    void builderDefaults() {
        Options opts = Options.builder("eu-lf_test_key_123").build();
        assertEquals("eu-lf_test_key_123", opts.apiKey);
        assertEquals("", opts.source);
        assertEquals("", opts.environment);
        assertEquals("", opts.release);
        assertEquals("", opts.node);
        assertEquals(1000, opts.queueSize);
        assertEquals(5000, opts.flushIntervalMs);
        assertEquals(100, opts.batchSize);
        assertEquals(2, opts.workerCount);
        assertEquals(3, opts.maxRetries);
        assertEquals(30000, opts.httpTimeoutMs);
        assertTrue(opts.failsafe);
        assertTrue(opts.enableCompression);
        assertEquals(1.0, opts.sampleRate);
        assertEquals(100, opts.maxBreadcrumbs);
    }

    @Test
    void builderCustomValues() {
        Options opts = Options.builder("eu-lf_test_key_123")
                .source("my-app")
                .environment("production")
                .release("v1.2.3")
                .node("node-01")
                .queueSize(2000)
                .flushInterval(3000)
                .batchSize(50)
                .workerCount(4)
                .maxRetries(5)
                .httpTimeout(60000)
                .failsafe(false)
                .enableCompression(false)
                .sampleRate(0.5)
                .maxBreadcrumbs(200)
                .build();

        assertEquals("my-app", opts.source);
        assertEquals("production", opts.environment);
        assertEquals("v1.2.3", opts.release);
        assertEquals("node-01", opts.node);
        assertEquals(2000, opts.queueSize);
        assertEquals(3000, opts.flushIntervalMs);
        assertEquals(50, opts.batchSize);
        assertEquals(4, opts.workerCount);
        assertEquals(5, opts.maxRetries);
        assertEquals(60000, opts.httpTimeoutMs);
        assertFalse(opts.failsafe);
        assertFalse(opts.enableCompression);
        assertEquals(0.5, opts.sampleRate);
        assertEquals(200, opts.maxBreadcrumbs);
    }

    @Test
    void builderWithHooks() {
        Options opts = Options.builder("eu-lf_test_key_123")
                .beforeSend(p -> p)
                .beforeSendLog(p -> null)  // drops all logs
                .beforeSendMetric(p -> p)
                .build();

        assertNotNull(opts.beforeSend);
        assertNotNull(opts.beforeSendLog);
        assertNotNull(opts.beforeSendMetric);
        assertNull(opts.beforeSendEvent);
        assertNull(opts.beforeSendAudit);
    }

    @Test
    void builderWithNullValues() {
        Options opts = Options.builder("eu-lf_test_key_123")
                .source(null)
                .environment(null)
                .release(null)
                .node(null)
                .build();

        assertEquals("", opts.source);
        assertEquals("", opts.environment);
        assertEquals("", opts.release);
        assertEquals("", opts.node);
    }

    @Test
    void fromEnvironmentRequiresApiKey() {
        // LOGFLUX_API_KEY is not set in test env
        assertThrows(IllegalArgumentException.class, Options::fromEnvironment);
    }

    @Test
    void apiKeyValidation() {
        // Valid keys
        assertDoesNotThrow(() -> Client.validateApiKey("eu-lf_test123"));
        assertDoesNotThrow(() -> Client.validateApiKey("us-lf_test123"));
        assertDoesNotThrow(() -> Client.validateApiKey("ca-lf_test123"));
        assertDoesNotThrow(() -> Client.validateApiKey("au-lf_test123"));
        assertDoesNotThrow(() -> Client.validateApiKey("ap-lf_test123"));

        // Invalid keys
        assertThrows(IllegalArgumentException.class, () -> Client.validateApiKey(null));
        assertThrows(IllegalArgumentException.class, () -> Client.validateApiKey(""));
        assertThrows(IllegalArgumentException.class, () -> Client.validateApiKey("noregion"));
        assertThrows(IllegalArgumentException.class, () -> Client.validateApiKey("xx-lf_test"));
        assertThrows(IllegalArgumentException.class, () -> Client.validateApiKey("eu-notlf_test"));
        assertThrows(IllegalArgumentException.class, () -> Client.validateApiKey("eu-lf_"));
    }
}
