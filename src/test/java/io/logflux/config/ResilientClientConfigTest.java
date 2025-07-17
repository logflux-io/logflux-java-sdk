package io.logflux.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ResilientClientConfigTest {

    @Test
    void testConstructorWithValidParameters() {
        ResilientClientConfig config = new ResilientClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123",
            Duration.ofSeconds(30),
            1000,
            Duration.ofSeconds(5),
            2,
            true,
            5,
            Duration.ofMillis(100),
            Duration.ofSeconds(30),
            2.0,
            true
        );

        assertEquals("https://c00000.ingest.eu.logflux.io", config.getServerUrl());
        assertEquals("node1", config.getNode());
        assertEquals("lf_test123", config.getApiKey());
        assertEquals("secret123", config.getSecret());
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertEquals(1000, config.getQueueSize());
        assertEquals(Duration.ofSeconds(5), config.getFlushInterval());
        assertEquals(2, config.getWorkerCount());
        assertTrue(config.isFailsafeMode());
        assertEquals(5, config.getMaxRetries());
        assertEquals(Duration.ofMillis(100), config.getInitialDelay());
        assertEquals(Duration.ofSeconds(30), config.getMaxDelay());
        assertEquals(2.0, config.getBackoffFactor(), 0.001);
        assertTrue(config.isJitterEnabled());
    }

    @Test
    void testBuilderPattern() {
        ResilientClientConfig config = new ResilientClientConfig.Builder()
            .serverUrl("https://c00000.ingest.eu.logflux.io")
            .node("node1")
            .apiKey("lf_test123")
            .secret("secret123")
            .timeout(Duration.ofSeconds(45))
            .queueSize(2000)
            .flushInterval(Duration.ofSeconds(10))
            .workerCount(4)
            .failsafeMode(false)
            .maxRetries(3)
            .initialDelay(Duration.ofMillis(200))
            .maxDelay(Duration.ofSeconds(60))
            .backoffFactor(1.5)
            .jitterEnabled(false)
            .build();

        assertEquals("https://c00000.ingest.eu.logflux.io", config.getServerUrl());
        assertEquals("node1", config.getNode());
        assertEquals("lf_test123", config.getApiKey());
        assertEquals("secret123", config.getSecret());
        assertEquals(Duration.ofSeconds(45), config.getTimeout());
        assertEquals(2000, config.getQueueSize());
        assertEquals(Duration.ofSeconds(10), config.getFlushInterval());
        assertEquals(4, config.getWorkerCount());
        assertFalse(config.isFailsafeMode());
        assertEquals(3, config.getMaxRetries());
        assertEquals(Duration.ofMillis(200), config.getInitialDelay());
        assertEquals(Duration.ofSeconds(60), config.getMaxDelay());
        assertEquals(1.5, config.getBackoffFactor(), 0.001);
        assertFalse(config.isJitterEnabled());
    }

    @Test
    void testBuilderWithDefaults() {
        ResilientClientConfig config = new ResilientClientConfig.Builder()
            .serverUrl("https://c00000.ingest.eu.logflux.io")
            .node("node1")
            .apiKey("lf_test123")
            .secret("secret123")
            .build();

        assertEquals("https://c00000.ingest.eu.logflux.io", config.getServerUrl());
        assertEquals("node1", config.getNode());
        assertEquals("lf_test123", config.getApiKey());
        assertEquals("secret123", config.getSecret());
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertEquals(1000, config.getQueueSize());
        assertEquals(Duration.ofSeconds(5), config.getFlushInterval());
        assertEquals(2, config.getWorkerCount());
        assertTrue(config.isFailsafeMode());
        assertEquals(5, config.getMaxRetries());
        assertEquals(Duration.ofMillis(100), config.getInitialDelay());
        assertEquals(Duration.ofSeconds(30), config.getMaxDelay());
        assertEquals(2.0, config.getBackoffFactor(), 0.001);
        assertTrue(config.isJitterEnabled());
    }

    @Test
    void testDefaultConfig() {
        ResilientClientConfig config = ResilientClientConfig.defaultConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        assertEquals("https://c00000.ingest.eu.logflux.io", config.getServerUrl());
        assertEquals("node1", config.getNode());
        assertEquals("lf_test123", config.getApiKey());
        assertEquals("secret123", config.getSecret());
        assertEquals(Duration.ofSeconds(30), config.getTimeout());
        assertEquals(1000, config.getQueueSize());
        assertEquals(Duration.ofSeconds(5), config.getFlushInterval());
        assertEquals(2, config.getWorkerCount());
        assertTrue(config.isFailsafeMode());
        assertEquals(5, config.getMaxRetries());
        assertEquals(Duration.ofMillis(100), config.getInitialDelay());
        assertEquals(Duration.ofSeconds(30), config.getMaxDelay());
        assertEquals(2.0, config.getBackoffFactor(), 0.001);
        assertTrue(config.isJitterEnabled());
    }

    @Test
    void testToClientConfig() {
        ResilientClientConfig resilientConfig = ResilientClientConfig.defaultConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        ClientConfig clientConfig = resilientConfig.toClientConfig();

        assertEquals(resilientConfig.getServerUrl(), clientConfig.getServerUrl());
        assertEquals(resilientConfig.getNode(), clientConfig.getNode());
        assertEquals(resilientConfig.getApiKey(), clientConfig.getApiKey());
        assertEquals(resilientConfig.getSecret(), clientConfig.getSecret());
        assertEquals(resilientConfig.getTimeout(), clientConfig.getTimeout());
    }

    @Test
    void testValidationWithNullParameters() {
        assertThrows(NullPointerException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl(null)
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .build()
        );

        assertThrows(NullPointerException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node(null)
                .apiKey("lf_key")
                .secret("secret")
                .build()
        );
    }

    @Test
    void testValidationWithEmptyParameters() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("")
                .apiKey("lf_key")
                .secret("secret")
                .build()
        );
    }

    @Test
    void testValidationWithInvalidApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("invalid_key")
                .secret("secret")
                .build()
        );
    }

    @Test
    void testValidationWithInvalidServerUrl() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("not-a-url")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .build()
        );
    }

    @Test
    void testValidationWithInvalidQueueSize() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .queueSize(0)
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .queueSize(-1)
                .build()
        );
    }

    @Test
    void testValidationWithInvalidWorkerCount() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .workerCount(0)
                .build()
        );
    }

    @Test
    void testValidationWithInvalidMaxRetries() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .maxRetries(-1)
                .build()
        );
    }

    @Test
    void testValidationWithInvalidBackoffFactor() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .backoffFactor(0.5)
                .build()
        );
    }

    @Test
    void testValidationWithInvalidTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .timeout(Duration.ZERO)
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .timeout(Duration.ofSeconds(-1))
                .build()
        );
    }

    @Test
    void testValidationWithInvalidFlushInterval() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .flushInterval(Duration.ofSeconds(-1))
                .build()
        );
    }

    @Test
    void testValidationWithInvalidDelays() {
        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .initialDelay(Duration.ofSeconds(-1))
                .build()
        );

        assertThrows(IllegalArgumentException.class, () ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .maxDelay(Duration.ofSeconds(-1))
                .build()
        );
    }

    @Test
    void testValidFlushIntervalZero() {
        // Zero flush interval should be valid (means no automatic flushing)
        assertDoesNotThrow(() ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .flushInterval(Duration.ZERO)
                .build()
        );
    }

    @Test
    void testValidDelaysZero() {
        // Zero delays should be valid
        assertDoesNotThrow(() ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .initialDelay(Duration.ZERO)
                .maxDelay(Duration.ZERO)
                .build()
        );
    }

    @Test
    void testValidMaxRetriesZero() {
        // Zero max retries should be valid (means no retries)
        assertDoesNotThrow(() ->
            new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node("node")
                .apiKey("lf_key")
                .secret("secret")
                .maxRetries(0)
                .build()
        );
    }
}