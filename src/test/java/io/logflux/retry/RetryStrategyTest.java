package io.logflux.retry;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryStrategyTest {

    @Test
    void testConstructor() {
        RetryStrategy strategy = new RetryStrategy(
            3,
            Duration.ofMillis(100),
            Duration.ofSeconds(10),
            2.0,
            true
        );

        assertEquals(3, strategy.getMaxRetries());
        assertEquals(Duration.ofMillis(100), strategy.getInitialDelay());
        assertEquals(Duration.ofSeconds(10), strategy.getMaxDelay());
        assertEquals(2.0, strategy.getBackoffFactor(), 0.001);
        assertTrue(strategy.isJitterEnabled());
    }

    @Test
    void testDefaultStrategy() {
        RetryStrategy strategy = RetryStrategy.defaultStrategy();

        assertEquals(5, strategy.getMaxRetries());
        assertEquals(Duration.ofMillis(100), strategy.getInitialDelay());
        assertEquals(Duration.ofSeconds(30), strategy.getMaxDelay());
        assertEquals(2.0, strategy.getBackoffFactor(), 0.001);
        assertTrue(strategy.isJitterEnabled());
    }

    @Test
    void testCalculateDelayWithoutJitter() {
        RetryStrategy strategy = new RetryStrategy(
            5,
            Duration.ofMillis(100),
            Duration.ofSeconds(10),
            2.0,
            false
        );

        assertEquals(Duration.ofMillis(100), strategy.calculateDelay(0));
        assertEquals(Duration.ofMillis(200), strategy.calculateDelay(1));
        assertEquals(Duration.ofMillis(400), strategy.calculateDelay(2));
        assertEquals(Duration.ofMillis(800), strategy.calculateDelay(3));
        assertEquals(Duration.ofMillis(1600), strategy.calculateDelay(4));
        
        // For attempt 5, which is >= maxRetries, should return maxDelay
        assertEquals(Duration.ofSeconds(10), strategy.calculateDelay(5));
        
        // Should be capped at max delay
        assertEquals(Duration.ofSeconds(10), strategy.calculateDelay(10));
    }

    @Test
    void testCalculateDelayWithJitter() {
        RetryStrategy strategy = new RetryStrategy(
            5,
            Duration.ofMillis(100),
            Duration.ofSeconds(10),
            2.0,
            true
        );

        for (int i = 0; i < 10; i++) {
            Duration delay = strategy.calculateDelay(i);
            assertTrue(delay.toMillis() >= 0);
            assertTrue(delay.toMillis() <= strategy.getMaxDelay().toMillis() * 1.1);
        }
    }

    @Test
    void testCalculateDelayWithNegativeAttempt() {
        RetryStrategy strategy = RetryStrategy.defaultStrategy();
        assertEquals(Duration.ZERO, strategy.calculateDelay(-1));
    }

    @Test
    void testShouldRetry() {
        RetryStrategy strategy = new RetryStrategy(3, Duration.ofMillis(100), Duration.ofSeconds(10), 2.0, false);

        assertTrue(strategy.shouldRetry(0));
        assertTrue(strategy.shouldRetry(1));
        assertTrue(strategy.shouldRetry(2));
        assertFalse(strategy.shouldRetry(3));
        assertFalse(strategy.shouldRetry(4));
    }

    @Test
    void testIsRetryableWithNull() {
        RetryStrategy strategy = RetryStrategy.defaultStrategy();
        assertFalse(strategy.isRetryable(null));
    }

    @Test
    void testIsRetryableWithNullMessage() {
        RetryStrategy strategy = RetryStrategy.defaultStrategy();
        Exception e = new Exception((String) null);
        assertFalse(strategy.isRetryable(e));
    }

    @Test
    void testIsRetryableWithNetworkErrors() {
        RetryStrategy strategy = RetryStrategy.defaultStrategy();

        assertTrue(strategy.isRetryable(new IOException("connection refused")));
        assertTrue(strategy.isRetryable(new IOException("connection reset")));
        assertTrue(strategy.isRetryable(new SocketTimeoutException("connection timed out")));
        assertTrue(strategy.isRetryable(new IOException("read timeout")));
        assertTrue(strategy.isRetryable(new IOException("network is unreachable")));
        assertTrue(strategy.isRetryable(new IOException("no route to host")));
        assertTrue(strategy.isRetryable(new IOException("service temporarily unavailable")));
        assertTrue(strategy.isRetryable(new IOException("http 500 internal server error")));
        assertTrue(strategy.isRetryable(new IOException("http 503 service unavailable")));
        assertTrue(strategy.isRetryable(new IOException("http 429 too many requests")));
    }

    @Test
    void testIsRetryableWithNonRetryableErrors() {
        RetryStrategy strategy = RetryStrategy.defaultStrategy();

        assertFalse(strategy.isRetryable(new IllegalArgumentException("Invalid argument")));
        assertFalse(strategy.isRetryable(new NullPointerException("Null value")));
        assertFalse(strategy.isRetryable(new IOException("HTTP 400 Bad Request")));
        assertFalse(strategy.isRetryable(new IOException("HTTP 401 Unauthorized")));
        assertFalse(strategy.isRetryable(new IOException("File not found")));
    }

    @Test
    void testExecuteSuccess() throws Exception {
        RetryStrategy strategy = new RetryStrategy(3, Duration.ZERO, Duration.ZERO, 2.0, false);
        AtomicInteger attempts = new AtomicInteger(0);

        String result = strategy.execute(() -> {
            attempts.incrementAndGet();
            return "success";
        });

        assertEquals("success", result);
        assertEquals(1, attempts.get());
    }

    @Test
    void testExecuteWithRetries() throws Exception {
        RetryStrategy strategy = new RetryStrategy(3, Duration.ZERO, Duration.ZERO, 2.0, false);
        AtomicInteger attempts = new AtomicInteger(0);

        String result = strategy.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new IOException("Connection refused");
            }
            return "success after retries";
        });

        assertEquals("success after retries", result);
        assertEquals(3, attempts.get());
    }

    @Test
    void testExecuteExhaustsRetries() {
        RetryStrategy strategy = new RetryStrategy(3, Duration.ZERO, Duration.ZERO, 2.0, false);
        AtomicInteger attempts = new AtomicInteger(0);

        Exception exception = assertThrows(IOException.class, () -> {
            strategy.execute(() -> {
                attempts.incrementAndGet();
                throw new IOException("Connection refused");
            });
        });

        assertEquals("Connection refused", exception.getMessage());
        assertEquals(4, attempts.get()); // Initial attempt + 3 retries
    }

    @Test
    void testExecuteWithNonRetryableException() {
        RetryStrategy strategy = new RetryStrategy(3, Duration.ZERO, Duration.ZERO, 2.0, false);
        AtomicInteger attempts = new AtomicInteger(0);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            strategy.execute(() -> {
                attempts.incrementAndGet();
                throw new IllegalArgumentException("Invalid argument");
            });
        });

        assertEquals("Invalid argument", exception.getMessage());
        assertEquals(1, attempts.get()); // Should not retry
    }

    @Test
    void testExecuteWithDelay() throws Exception {
        RetryStrategy strategy = new RetryStrategy(
            2,
            Duration.ofMillis(50),
            Duration.ofMillis(200),
            2.0,
            false
        );
        AtomicInteger attempts = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        String result = strategy.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new IOException("Connection timeout");
            }
            return "success";
        });

        long duration = System.currentTimeMillis() - startTime;

        assertEquals("success", result);
        assertEquals(3, attempts.get());
        // Should have delays of 50ms and 100ms
        assertTrue(duration >= 150);
    }

    @Test
    void testExecuteWithMaxDelayCapOooo() throws Exception {
        RetryStrategy strategy = new RetryStrategy(
            10,
            Duration.ofMillis(100),
            Duration.ofMillis(200),
            10.0, // Very high backoff factor
            false
        );

        // Verify delays are capped at max delay
        for (int i = 0; i < 10; i++) {
            Duration delay = strategy.calculateDelay(i);
            assertTrue(delay.toMillis() <= 200);
        }
    }

    @Test
    void testExecuteWithZeroRetries() {
        RetryStrategy strategy = new RetryStrategy(0, Duration.ZERO, Duration.ZERO, 2.0, false);
        AtomicInteger attempts = new AtomicInteger(0);

        Exception exception = assertThrows(IOException.class, () -> {
            strategy.execute(() -> {
                attempts.incrementAndGet();
                throw new IOException("Connection refused");
            });
        });

        assertEquals("Connection refused", exception.getMessage());
        assertEquals(1, attempts.get()); // Only initial attempt, no retries
    }
}