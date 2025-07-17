package io.logflux.adapters;

import io.logflux.client.ResilientClient;
import io.logflux.config.ResilientClientConfig;
import io.logflux.util.TestConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for logger adapters.
 */
class AdapterTest {
    private ResilientClient client;

    @BeforeEach
    void setUp() throws Exception {
        ResilientClientConfig config = new ResilientClientConfig.Builder()
                .serverUrl("https://c00000.ingest.eu.logflux.io")
                .node(TestConstants.TEST_NODE)
                .apiKey(TestConstants.TEST_API_KEY)
                .secret(TestConstants.TEST_SECRET)
                .timeout(Duration.ofSeconds(10))
                .queueSize(100)
                .flushInterval(Duration.ofMillis(100))
                .failsafeMode(true)
                .build();
        
        client = new ResilientClient(config);
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void shouldWorkWithSlf4jAdapter() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "slf4j-test");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            logger.info("SLF4J info message");
            logger.warn("SLF4J warning message");
            logger.error("SLF4J error message");
            logger.debug("SLF4J debug message");
        });
        
        assertEquals("slf4j-test", logger.getName());
    }

    @Test
    void shouldWorkWithSlf4jParameterizedLogging() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "slf4j-params");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            logger.info("User {} logged in from IP {}", "john123", "192.168.1.1");
            logger.warn("Rate limit reached: {} requests in {} seconds", 1000, 60);
            logger.error("Error processing request {}: {}", "req123", "timeout");
        });
    }

    @Test
    void shouldWorkWithSlf4jWithException() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "slf4j-exception");
        Exception testException = new RuntimeException("Test exception");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            logger.error("An error occurred", testException);
        });
    }

    @Test
    void shouldWorkWithJulAdapter() {
        // Given
        Logger julLogger = JulAdapter.createLogger(client, "jul-test");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            julLogger.info("JUL info message");
            julLogger.warning("JUL warning message");
            julLogger.severe("JUL severe message");
            julLogger.fine("JUL fine message");
        });
        
        assertEquals("jul-test", julLogger.getName());
    }

    @Test
    void shouldWorkWithJulAdapterDirectly() {
        // Given
        Logger julLogger = Logger.getLogger("jul-direct");
        JulAdapter adapter = new JulAdapter(client);
        julLogger.addHandler(adapter);
        julLogger.setUseParentHandlers(false);

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            julLogger.info("JUL direct info message");
            julLogger.warning("JUL direct warning message");
        });
    }

    @Test
    void shouldWorkWithLog4jAdapter() {
        // Given & When
        Log4jAdapter adapter = Log4jAdapter.createAppender(client, "log4j-test");

        // Then
        assertNotNull(adapter);
        assertEquals("log4j-test", adapter.getName());
    }

    @Test
    void shouldHandleMultipleAdaptersSimultaneously() {
        // Given
        Slf4jAdapter slf4jLogger = new Slf4jAdapter(client, "multi-slf4j");
        Logger julLogger = JulAdapter.createLogger(client, "multi-jul");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            slf4jLogger.info("SLF4J message");
            julLogger.info("JUL message");
            slf4jLogger.warn("SLF4J warning");
            julLogger.warning("JUL warning");
        });
    }

    @Test
    void shouldHandleHighVolumeLoggingFromAdapters() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "high-volume");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 50; i++) {
                logger.info("High volume message {}", i + 1);
            }
        });
    }

    @Test
    void shouldHandleAdapterErrorsGracefully() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "error-handling");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            logger.info("Test message with failures");
            logger.error("Another test message");
        });
    }

    @Test
    void shouldSupportSlf4jLogLevelChecking() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "level-checking");

        // When & Then
        assertTrue(logger.isTraceEnabled());
        assertTrue(logger.isDebugEnabled());
        assertTrue(logger.isInfoEnabled());
        assertTrue(logger.isWarnEnabled());
        assertTrue(logger.isErrorEnabled());
        
        // With markers
        assertTrue(logger.isTraceEnabled(null));
        assertTrue(logger.isDebugEnabled(null));
        assertTrue(logger.isInfoEnabled(null));
        assertTrue(logger.isWarnEnabled(null));
        assertTrue(logger.isErrorEnabled(null));
    }

    @Test
    void shouldFormatSlf4jMessagesCorrectly() {
        // Given
        Slf4jAdapter logger = new Slf4jAdapter(client, "formatting");

        // When & Then - Should not throw in failsafe mode
        assertDoesNotThrow(() -> {
            logger.info("Simple message");
            logger.info("Message with one param: {}", "value");
            logger.info("Message with two params: {} and {}", "value1", "value2");
            logger.info("Message with three params: {}, {}, {}", "value1", "value2", "value3");
        });
    }
}