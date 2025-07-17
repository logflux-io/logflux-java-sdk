package io.logflux.retry;

import java.time.Duration;
import java.util.Random;

/**
 * Implements exponential backoff retry strategy with jitter.
 */
public class RetryStrategy {
    private final int maxRetries;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffFactor;
    private final boolean jitterEnabled;
    private final Random random;

    /**
     * Creates a new RetryStrategy with the specified parameters.
     *
     * @param maxRetries     Maximum number of retry attempts
     * @param initialDelay   Initial delay before first retry
     * @param maxDelay       Maximum delay between retries
     * @param backoffFactor  Multiplier for exponential backoff
     * @param jitterEnabled  Whether to add random jitter to delays
     */
    public RetryStrategy(int maxRetries, Duration initialDelay, Duration maxDelay, 
                        double backoffFactor, boolean jitterEnabled) {
        this.maxRetries = maxRetries;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.backoffFactor = backoffFactor;
        this.jitterEnabled = jitterEnabled;
        this.random = jitterEnabled ? new Random() : null;
    }

    /**
     * Creates a default RetryStrategy with reasonable defaults.
     *
     * @return A new RetryStrategy with default settings
     */
    public static RetryStrategy defaultStrategy() {
        return new RetryStrategy(
            5,                          // maxRetries
            Duration.ofMillis(100),     // initialDelay
            Duration.ofSeconds(30),     // maxDelay
            2.0,                        // backoffFactor
            true                        // jitterEnabled
        );
    }

    /**
     * Calculates the delay for the specified retry attempt.
     *
     * @param attempt The retry attempt number (0-based)
     * @return The delay duration
     */
    public Duration calculateDelay(int attempt) {
        if (attempt < 0) {
            return Duration.ZERO;
        }
        
        if (attempt >= maxRetries) {
            return maxDelay;
        }
        
        // Calculate exponential backoff
        double delayMs = initialDelay.toMillis() * Math.pow(backoffFactor, attempt);
        
        // Cap at max delay
        delayMs = Math.min(delayMs, maxDelay.toMillis());
        
        // Add jitter if enabled
        if (jitterEnabled && random != null) {
            // Add up to 10% jitter
            double jitter = (random.nextDouble() - 0.5) * 0.1 * delayMs;
            delayMs += jitter;
        }
        
        return Duration.ofMillis(Math.max(0, (long) delayMs));
    }

    /**
     * Checks if retry should be attempted for the given attempt number.
     *
     * @param attempt The retry attempt number (0-based)
     * @return true if retry should be attempted
     */
    public boolean shouldRetry(int attempt) {
        return attempt < maxRetries;
    }

    /**
     * Checks if an exception is retryable.
     *
     * @param exception The exception to check
     * @return true if the exception is retryable
     */
    public boolean isRetryable(Exception exception) {
        if (exception == null) {
            return false;
        }
        
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Network-related errors that are typically retryable
        return lowerMessage.contains("connection refused") ||
               lowerMessage.contains("connection reset") ||
               lowerMessage.contains("connection timed out") ||
               lowerMessage.contains("timeout") ||
               lowerMessage.contains("network is unreachable") ||
               lowerMessage.contains("no route to host") ||
               lowerMessage.contains("temporarily unavailable") ||
               lowerMessage.contains("http 5") || // 5xx HTTP errors
               lowerMessage.contains("http 429"); // Rate limiting
    }

    /**
     * Executes a retry operation with exponential backoff.
     *
     * @param operation The operation to retry
     * @param <T>       The return type of the operation
     * @return The result of the operation
     * @throws Exception if all retry attempts fail
     */
    public <T> T execute(RetryableOperation<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxRetries && isRetryable(e)) {
                    Duration delay = calculateDelay(attempt);
                    if (!delay.isZero()) {
                        Thread.sleep(delay.toMillis());
                    }
                } else {
                    break;
                }
            }
        }
        
        throw lastException != null ? lastException : new RuntimeException("Operation failed after retries");
    }

    /**
     * Gets the maximum number of retries.
     *
     * @return The maximum retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the initial delay.
     *
     * @return The initial delay
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * Gets the maximum delay.
     *
     * @return The maximum delay
     */
    public Duration getMaxDelay() {
        return maxDelay;
    }

    /**
     * Gets the backoff factor.
     *
     * @return The backoff factor
     */
    public double getBackoffFactor() {
        return backoffFactor;
    }

    /**
     * Checks if jitter is enabled.
     *
     * @return true if jitter is enabled
     */
    public boolean isJitterEnabled() {
        return jitterEnabled;
    }

    /**
     * Functional interface for retryable operations.
     *
     * @param <T> The return type
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}