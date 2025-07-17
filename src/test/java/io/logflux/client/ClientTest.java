package io.logflux.client;

import io.logflux.config.ClientConfig;
import io.logflux.models.LogLevel;
import io.logflux.models.LogEntry;
import io.logflux.models.LogResponse;
import io.logflux.client.LogFluxException;
import io.logflux.crypto.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ClientTest {

    private Client client;
    private ClientConfig config;

    @BeforeEach
    void setUp() {
        config = new ClientConfig(
            "https://c00000.ingest.eu.logflux.io",
            "test-node",
            "lf_test123",
            "test-secret",
            Duration.ofSeconds(5)
        );
        client = new Client(config);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testCreateClient() {
        assertNotNull(client);
        assertEquals(config, client.getConfig());
    }

    @Test
    void testCreateClientWithStaticMethod() {
        Client staticClient = Client.create(
            "https://c00000.ingest.eu.logflux.io",
            "test-node",
            "lf_test123",
            "test-secret"
        );
        
        assertNotNull(staticClient);
        assertEquals("https://c00000.ingest.eu.logflux.io", staticClient.getConfig().getServerUrl());
        assertEquals("test-node", staticClient.getConfig().getNode());
        assertEquals("lf_test123", staticClient.getConfig().getApiKey());
        assertEquals("test-secret", staticClient.getConfig().getSecret());
        
        try {
            staticClient.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    @Test
    void testSendLogWithDebugLevel() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.sendLog("Test debug message", LogLevel.DEBUG);
        });
    }

    @Test
    void testSendLogWithInfoLevel() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.sendLog("Test info message", LogLevel.INFO);
        });
    }

    @Test
    void testSendLogWithWarnLevel() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.sendLog("Test warning message", LogLevel.WARN);
        });
    }

    @Test
    void testSendLogWithErrorLevel() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.sendLog("Test error message", LogLevel.ERROR);
        });
    }

    @Test
    void testSendLogWithFatalLevel() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.sendLog("Test fatal message", LogLevel.FATAL);
        });
    }

    @Test
    void testSendLogWithTimestamp() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.sendLog("Test message", LogLevel.INFO, Instant.now());
        });
    }

    @Test
    void testSendLogWithNullMessage() {
        assertThrows(EncryptionException.class, () -> {
            client.sendLog(null, LogLevel.INFO);
        });
    }

    @Test
    void testSendLogWithNullLevel() {
        assertThrows(NullPointerException.class, () -> {
            client.sendLog("Test message", null);
        });
    }

    @Test
    void testSendLogBatch() {
        // Note: This would fail without a real server
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry("test-node", "encrypted1", LogLevel.INFO, Instant.now()));
        entries.add(new LogEntry("test-node", "encrypted2", LogLevel.WARN, Instant.now()));
        
        assertThrows(Exception.class, () -> {
            client.sendLogBatch(entries);
        });
    }

    @Test
    void testSendLogBatchWithEmptyList() {
        List<LogEntry> entries = new ArrayList<>();
        
        assertThrows(Exception.class, () -> {
            client.sendLogBatch(entries);
        });
    }

    @Test
    void testSendLogBatchWithNullList() {
        assertThrows(LogFluxException.class, () -> {
            client.sendLogBatch(null);
        });
    }

    @Test
    void testHealth() {
        // Note: This would fail without a real server
        assertThrows(Exception.class, () -> {
            client.health();
        });
    }

    @Test
    void testAutoCloseable() throws Exception {
        try (Client autoClient = new Client(config)) {
            assertNotNull(autoClient);
        }
        // Client should be closed automatically
    }

    @Test
    void testGetConfig() {
        ClientConfig retrievedConfig = client.getConfig();
        assertEquals(config, retrievedConfig);
        assertEquals("https://c00000.ingest.eu.logflux.io", retrievedConfig.getServerUrl());
        assertEquals("test-node", retrievedConfig.getNode());
        assertEquals("lf_test123", retrievedConfig.getApiKey());
        assertEquals("test-secret", retrievedConfig.getSecret());
    }

    @Test
    void testAsyncMethods() {
        // Test that async methods return CompletableFuture
        LogEntry entry = new LogEntry("test-node", "encrypted", LogLevel.DEBUG, Instant.now());
        CompletableFuture<LogResponse> future = client.sendLogAsync(entry);
        assertNotNull(future);
        assertTrue(future instanceof CompletableFuture);
        
        // Cancel the future to prevent hanging
        future.cancel(true);
    }

    @Test
    void testClientWithInvalidConfig() {
        assertThrows(NullPointerException.class, () -> {
            new Client(null);
        });
    }
}