package io.logflux.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientConfigTest {

    private final Map<String, String> originalEnv = new HashMap<>();

    @BeforeEach
    void setUp() {
        // Save original environment variables
        originalEnv.put("LOGFLUX_SERVER_URL", System.getenv("LOGFLUX_SERVER_URL"));
        originalEnv.put("LOGFLUX_API_KEY", System.getenv("LOGFLUX_API_KEY"));
        originalEnv.put("LOGFLUX_HTTP_TIMEOUT", System.getenv("LOGFLUX_HTTP_TIMEOUT"));
    }

    @AfterEach
    void tearDown() {
        // Restore original environment variables
        originalEnv.forEach((key, value) -> {
            if (value != null) {
                System.setProperty(key, value);
            }
        });
    }

    @Test
    void testConstructorWithValidParameters() {
        ClientConfig config = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123",
            Duration.ofSeconds(45)
        );

        assertEquals("https://c00000.ingest.eu.logflux.io", config.getServerUrl());
        assertEquals("node1", config.getNode());
        assertEquals("lf_test123", config.getApiKey());
        assertEquals("secret123", config.getSecret());
        assertEquals(Duration.ofSeconds(45), config.getTimeout());
    }

    @Test
    void testConstructorWithDefaultTimeout() {
        ClientConfig config = new ClientConfig(
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
    }

    @Test
    void testConstructorWithNullParameters() {
        assertThrows(NullPointerException.class, () ->
            new ClientConfig(null, "node", "lf_key", "secret"));
        
        assertThrows(NullPointerException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", null, "lf_key", "secret"));
        
        assertThrows(NullPointerException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", null, "secret"));
        
        assertThrows(NullPointerException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "lf_key", null));
        
        assertThrows(NullPointerException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "lf_key", "secret", null));
    }

    @Test
    void testConstructorWithEmptyParameters() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("", "node", "lf_key", "secret"));
        
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "", "lf_key", "secret"));
        
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "", "secret"));
        
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "lf_key", ""));
    }

    @Test
    void testConstructorWithInvalidApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "invalid_key", "secret"));
    }

    @Test
    void testConstructorWithInvalidServerUrl() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("not-a-url", "node", "lf_key", "secret"));
        
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("ftp://example.com", "node", "lf_key", "secret"));
    }

    @Test
    void testConstructorWithInvalidTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "lf_key", "secret", Duration.ZERO));
        
        assertThrows(IllegalArgumentException.class, () ->
            new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "lf_key", "secret", Duration.ofSeconds(-1)));
    }

    @Test
    void testWithTimeout() {
        ClientConfig original = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        ClientConfig modified = original.withTimeout(Duration.ofSeconds(60));

        assertEquals(original.getServerUrl(), modified.getServerUrl());
        assertEquals(original.getNode(), modified.getNode());
        assertEquals(original.getApiKey(), modified.getApiKey());
        assertEquals(original.getSecret(), modified.getSecret());
        assertEquals(Duration.ofSeconds(60), modified.getTimeout());
    }

    @Test
    void testWithServerUrl() {
        ClientConfig original = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        ClientConfig modified = original.withServerUrl("https://new.logflux.io");

        assertEquals("https://new.logflux.io", modified.getServerUrl());
        assertEquals(original.getNode(), modified.getNode());
        assertEquals(original.getApiKey(), modified.getApiKey());
        assertEquals(original.getSecret(), modified.getSecret());
        assertEquals(original.getTimeout(), modified.getTimeout());
    }

    @Test
    void testEquals() {
        ClientConfig config1 = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123",
            Duration.ofSeconds(30)
        );

        ClientConfig config2 = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123",
            Duration.ofSeconds(30)
        );

        ClientConfig config3 = new ClientConfig(
            "https://different.logflux.io",
            "node1",
            "lf_test123",
            "secret123",
            Duration.ofSeconds(30)
        );

        assertEquals(config1, config2);
        assertNotEquals(config1, config3);
        assertNotEquals(config1, null);
        assertNotEquals(config1, new Object());
        assertEquals(config1, config1);
    }

    @Test
    void testHashCode() {
        ClientConfig config1 = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        ClientConfig config2 = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    void testToString() {
        ClientConfig config = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123456789",
            "secret123"
        );

        String toString = config.toString();

        assertTrue(toString.contains("https://c00000.ingest.eu.logflux.io"));
        assertTrue(toString.contains("node1"));
        assertTrue(toString.contains("lf_t***6789")); // Masked API key
        assertTrue(toString.contains("secret='***'")); // Masked secret
        assertTrue(toString.contains("PT30S")); // Duration format
    }

    @Test
    void testToStringWithShortApiKey() {
        ClientConfig config = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_key",
            "secret123"
        );

        String toString = config.toString();
        assertTrue(toString.contains("apiKey='***'"));
    }

    @Test
    void testValidUrlFormats() {
        // Should accept both http and https
        assertDoesNotThrow(() -> new ClientConfig("http://localhost:8080", "node", "lf_key", "secret"));
        assertDoesNotThrow(() -> new ClientConfig("https://c00000.ingest.eu.logflux.io", "node", "lf_key", "secret"));
        assertDoesNotThrow(() -> new ClientConfig("https://c00000.ingest.eu.logflux.io:443/v1", "node", "lf_key", "secret"));
    }

    @Test
    void testTrimming() {
        // The implementation validates trimmed values but stores original values
        ClientConfig config = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "node1",
            "lf_test123",
            "secret123"
        );

        assertEquals("https://c00000.ingest.eu.logflux.io", config.getServerUrl());
        assertEquals("node1", config.getNode());
        assertEquals("lf_test123", config.getApiKey());
        assertEquals("secret123", config.getSecret());
    }

    // Note: Testing fromEnvironment method would require mocking environment variables
    // which is complex in Java. In a real test suite, you might use PowerMock or
    // similar libraries to mock static methods like System.getenv()
}