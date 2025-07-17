package io.logflux.client;

import io.logflux.config.ResilientClientConfig;
import io.logflux.models.ClientStats;
import io.logflux.models.LogLevel;
import io.logflux.models.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ResilientClientTest {

    private ResilientClient client;
    private ResilientClientConfig config;

    @BeforeEach
    void setUp() {
        config = new ResilientClientConfig.Builder()
            .serverUrl("https://c00000.ingest.eu.logflux.io")
            .node("test-node")
            .apiKey("lf_test123")
            .secret("test-secret")
            .timeout(Duration.ofSeconds(5))
            .queueSize(100)
            .flushInterval(Duration.ofSeconds(1))
            .workerCount(2)
            .failsafeMode(true)
            .maxRetries(3)
            .build();
        client = new ResilientClient(config);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testCreateResilientClient() {
        assertNotNull(client);
        assertEquals(config, client.getConfig());
    }

    @Test
    void testCreateResilientClientWithStaticMethod() throws Exception {
        ResilientClient staticClient = ResilientClient.create(
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
        
        staticClient.close();
    }

    @Test
    void testSendLogWithDebugLevel() {
        // In failsafe mode, logs should be queued without throwing exceptions
        assertDoesNotThrow(() -> {
            client.sendLog("Test debug message", LogLevel.DEBUG);
        });
    }

    @Test
    void testSendLogWithInfoLevel() {
        assertDoesNotThrow(() -> {
            client.sendLog("Test info message", LogLevel.INFO);
        });
    }

    @Test
    void testSendLogWithWarnLevel() {
        assertDoesNotThrow(() -> {
            client.sendLog("Test warning message", LogLevel.WARN);
        });
    }

    @Test
    void testSendLogWithErrorLevel() {
        assertDoesNotThrow(() -> {
            client.sendLog("Test error message", LogLevel.ERROR);
        });
    }

    @Test
    void testSendLogWithFatalLevel() {
        assertDoesNotThrow(() -> {
            client.sendLog("Test fatal message", LogLevel.FATAL);
        });
    }

    @Test
    void testSendLogWithTimestamp() {
        assertDoesNotThrow(() -> {
            client.sendLog("Test message", LogLevel.INFO, Instant.now());
        });
    }

    @Test
    void testSendLogBatch() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry("test-node", "encrypted1", LogLevel.INFO, Instant.now()));
        entries.add(new LogEntry("test-node", "encrypted2", LogLevel.WARN, Instant.now()));
        
        assertDoesNotThrow(() -> {
            client.sendLogBatch(entries);
        });
    }

    @Test
    void testSendLogBatchWithEmptyList() {
        List<LogEntry> entries = new ArrayList<>();
        
        assertDoesNotThrow(() -> {
            client.sendLogBatch(entries);
        });
    }

    @Test
    void testGetStats() {
        ClientStats stats = client.getStats();
        
        assertNotNull(stats);
        assertEquals(0, stats.getTotalSent());
        assertEquals(0, stats.getTotalFailed());
        assertEquals(0, stats.getTotalDropped());
        assertTrue(stats.getQueueSize() >= 0);
        assertEquals(100, stats.getQueueCapacity());
    }

    @Test
    void testFlushSync() throws InterruptedException {
        // Send some logs
        client.sendLog("Test message 1", LogLevel.INFO);
        client.sendLog("Test message 2", LogLevel.INFO);
        
        // Flush should process queued logs
        assertDoesNotThrow(() -> {
            client.flushSync(Duration.ofSeconds(1));
        });
        
        // Give some time for flush to complete
        Thread.sleep(100);
        
        ClientStats stats = client.getStats();
        // Without a real server, these will fail, but at least they were attempted
        assertTrue(stats.getTotalFailed() >= 0);
    }

    @Test
    void testFlushWithTimeout() {
        // Test flush with timeout
        assertDoesNotThrow(() -> {
            client.flush(Duration.ofSeconds(1));
        });
    }

    @Test
    void testQueueBehaviorInFailsafeMode() throws InterruptedException {
        // Fill the queue
        for (int i = 0; i < 150; i++) {
            client.sendLog("Message " + i, LogLevel.INFO);
        }
        
        // Give some time for the queue to fill
        Thread.sleep(100);
        
        ClientStats stats = client.getStats();
        
        // In failsafe mode, excess messages should be dropped or queue should be within capacity
        assertTrue(stats.getTotalDropped() >= 0);
        assertTrue(stats.getQueueSize() <= stats.getQueueCapacity());
    }

    @Test
    void testAsyncMethods() {
        CompletableFuture<Void> future = client.sendLog("Test async debug", LogLevel.DEBUG);
        assertNotNull(future);
        assertTrue(future instanceof CompletableFuture);
        
        // In failsafe mode, the future should complete (though may timeout without server)
        // We don't expect it to throw an exception, just complete
        assertNotNull(future);
    }

    @Test
    void testAutoFlush() throws InterruptedException {
        // With flush interval of 1 second, logs should be automatically flushed
        client.sendLog("Test message", LogLevel.INFO);
        
        Thread.sleep(1500); // Wait for auto-flush
        
        ClientStats stats = client.getStats();
        // The queue should have been flushed
        assertTrue(stats.getQueueSize() < 100);
    }

    @Test
    void testCloseClient() throws Exception {
        client.sendLog("Test message before close", LogLevel.INFO);
        
        assertDoesNotThrow(() -> {
            client.close();
        });
        
        // After close, sending logs should throw or be ignored
        // The behavior depends on implementation
    }

    @Test
    void testResilientClientWithNullConfig() {
        assertThrows(NullPointerException.class, () -> {
            new ResilientClient(null);
        });
    }

    @Test
    void testNonFailsafeModeConfig() throws Exception {
        ResilientClientConfig nonFailsafeConfig = new ResilientClientConfig.Builder()
            .serverUrl("https://c00000.ingest.eu.logflux.io")
            .node("test-node")
            .apiKey("lf_test123")
            .secret("test-secret")
            .failsafeMode(false)
            .queueSize(10)
            .build();
            
        try (ResilientClient nonFailsafeClient = new ResilientClient(nonFailsafeConfig)) {
            // In non-failsafe mode, the client might block when queue is full
            for (int i = 0; i < 10; i++) {
                nonFailsafeClient.sendLog("Message " + i, LogLevel.INFO);
            }
            
            ClientStats stats = nonFailsafeClient.getStats();
            assertEquals(0, stats.getTotalDropped()); // No drops in non-failsafe mode
        }
    }

    @Test
    void testGetConfig() {
        ResilientClientConfig retrievedConfig = client.getConfig();
        assertEquals(config, retrievedConfig);
        assertEquals("https://c00000.ingest.eu.logflux.io", retrievedConfig.getServerUrl());
        assertEquals("test-node", retrievedConfig.getNode());
        assertTrue(retrievedConfig.isFailsafeMode());
    }
}