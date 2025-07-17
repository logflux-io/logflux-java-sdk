package io.logflux.client;

import io.logflux.config.ResilientClientConfig;
import io.logflux.crypto.EncryptionResult;
import io.logflux.crypto.Encryptor;
import io.logflux.models.ClientStats;
import io.logflux.models.LogEntry;
import io.logflux.models.LogLevel;
import io.logflux.models.LogResponse;
import io.logflux.queue.LogQueue;
import io.logflux.retry.RetryStrategy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Resilient LogFlux client with queuing, retry logic, and background workers.
 */
public class ResilientClient implements AutoCloseable {
    private final ResilientClientConfig config;
    private final Client basicClient;
    private final Encryptor encryptor;
    private final LogQueue queue;
    private final RetryStrategy retryStrategy;
    private final ExecutorService workerPool;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean running;
    private final AtomicLong totalSent;
    private final AtomicLong totalFailed;
    private final CountDownLatch shutdownLatch;

    /**
     * Creates a new ResilientClient with the specified configuration.
     *
     * @param config The resilient client configuration
     */
    public ResilientClient(ResilientClientConfig config) {
        this.config = config;
        this.basicClient = new Client(config.toClientConfig());
        this.encryptor = new Encryptor(config.getSecret());
        this.queue = new LogQueue(config.getQueueSize(), config.isFailsafeMode());
        this.retryStrategy = new RetryStrategy(
                config.getMaxRetries(),
                config.getInitialDelay(),
                config.getMaxDelay(),
                config.getBackoffFactor(),
                config.isJitterEnabled()
        );
        this.workerPool = Executors.newFixedThreadPool(config.getWorkerCount());
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.running = new AtomicBoolean(true);
        this.totalSent = new AtomicLong(0);
        this.totalFailed = new AtomicLong(0);
        this.shutdownLatch = new CountDownLatch(config.getWorkerCount());

        startWorkers();
        startFlushScheduler();
    }

    /**
     * Creates a new ResilientClient from environment variables.
     *
     * @param node   The node identifier
     * @param secret The encryption secret
     * @return A new ResilientClient
     */
    public static ResilientClient createFromEnv(String node, String secret) {
        return new ResilientClient(ResilientClientConfig.fromEnvironment(node, secret));
    }

    /**
     * Creates a new ResilientClient with default configuration.
     *
     * @param serverUrl The server URL
     * @param node      The node identifier
     * @param apiKey    The API key
     * @param secret    The encryption secret
     * @return A new ResilientClient
     */
    public static ResilientClient create(String serverUrl, String node, String apiKey, String secret) {
        return new ResilientClient(ResilientClientConfig.defaultConfig(serverUrl, node, apiKey, secret));
    }

    /**
     * Sends a log message asynchronously.
     *
     * @param message The log message
     * @param level   The log level
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> sendLog(String message, LogLevel level) {
        return sendLog(message, level, Instant.now());
    }

    /**
     * Sends a log message with timestamp asynchronously.
     *
     * @param message   The log message
     * @param level     The log level
     * @param timestamp The timestamp
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> sendLog(String message, LogLevel level, Instant timestamp) {
        return CompletableFuture.runAsync(() -> {
            try {
                EncryptionResult encryptionResult = encryptor.encryptToResult(message);
                LogEntry entry = new LogEntry(
                    config.getNode(),
                    encryptionResult.getEncryptedPayload(),
                    level,
                    timestamp,
                    encryptionResult.getEncryptionMode(),
                    encryptionResult.getIv(),
                    encryptionResult.getSalt()
                );
                
                if (!queue.offer(entry)) {
                    if (!config.isFailsafeMode()) {
                        throw new LogFluxException("Failed to queue log entry");
                    }
                    // In failsafe mode, silently drop the message
                }
            } catch (Exception e) {
                if (!config.isFailsafeMode()) {
                    throw new LogFluxException("Failed to send log", e);
                }
                // In failsafe mode, silently ignore errors
            }
        }, workerPool);
    }

    /**
     * Sends multiple log entries in a batch.
     *
     * @param entries The log entries
     * @return A CompletableFuture that completes when all entries are queued
     */
    public CompletableFuture<Void> sendLogBatch(List<LogEntry> entries) {
        return CompletableFuture.runAsync(() -> {
            for (LogEntry entry : entries) {
                try {
                    if (!queue.offer(entry)) {
                        if (!config.isFailsafeMode()) {
                            throw new LogFluxException("Failed to queue log entry");
                        }
                        // In failsafe mode, silently drop the message
                    }
                } catch (Exception e) {
                    if (!config.isFailsafeMode()) {
                        throw new LogFluxException("Failed to send log batch", e);
                    }
                    // In failsafe mode, silently ignore errors
                }
            }
        }, workerPool);
    }

    /**
     * Sends a debug level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> debug(String message) {
        return sendLog(message, LogLevel.DEBUG);
    }

    /**
     * Sends an info level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> info(String message) {
        return sendLog(message, LogLevel.INFO);
    }

    /**
     * Sends a notice level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> notice(String message) {
        return sendLog(message, LogLevel.NOTICE);
    }

    /**
     * Sends a warning level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> warning(String message) {
        return sendLog(message, LogLevel.WARNING);
    }

    /**
     * Sends an error level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> error(String message) {
        return sendLog(message, LogLevel.ERROR);
    }

    /**
     * Sends a critical level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> critical(String message) {
        return sendLog(message, LogLevel.CRITICAL);
    }

    /**
     * Sends an alert level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> alert(String message) {
        return sendLog(message, LogLevel.ALERT);
    }

    /**
     * Sends an emergency level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> emergency(String message) {
        return sendLog(message, LogLevel.EMERGENCY);
    }

    /**
     * Sends a warning level log message (alias for warning()).
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> warn(String message) {
        return warning(message);
    }

    /**
     * Sends a fatal level log message (alias for emergency()).
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public CompletableFuture<Void> fatal(String message) {
        return emergency(message);
    }

    /**
     * Flushes all queued log entries with the specified timeout.
     *
     * @param timeout The maximum time to wait for flush completion
     * @return A CompletableFuture that completes when all entries are flushed
     */
    public CompletableFuture<Void> flush(Duration timeout) {
        return CompletableFuture.runAsync(() -> {
            try {
                flushSync(timeout);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new LogFluxException("Flush interrupted", e);
            }
        }, workerPool);
    }

    /**
     * Synchronously flushes all queued log entries.
     *
     * @param timeout The maximum time to wait for flush completion
     * @throws InterruptedException if interrupted while waiting
     */
    public void flushSync(Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        
        while (queue.size() > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10); // Small delay to avoid busy waiting
        }
    }

    /**
     * Gets current client statistics.
     *
     * @return ClientStats with current metrics
     */
    public ClientStats getStats() {
        return new ClientStats(
                totalSent.get(),
                totalFailed.get(),
                queue.getTotalDropped(),
                queue.size(),
                queue.capacity()
        );
    }

    /**
     * Checks if the client is running.
     *
     * @return true if the client is running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Gets the client configuration.
     *
     * @return The client configuration
     */
    public ResilientClientConfig getConfig() {
        return config;
    }

    /**
     * Starts the background worker threads.
     */
    private void startWorkers() {
        for (int i = 0; i < config.getWorkerCount(); i++) {
            workerPool.submit(this::workerLoop);
        }
    }

    /**
     * Starts the periodic flush scheduler.
     */
    private void startFlushScheduler() {
        if (!config.getFlushInterval().isZero()) {
            scheduler.scheduleAtFixedRate(
                    this::periodicFlush,
                    config.getFlushInterval().toMillis(),
                    config.getFlushInterval().toMillis(),
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Main worker loop for processing queued log entries.
     */
    private void workerLoop() {
        try {
            while (running.get()) {
                try {
                    LogEntry entry = queue.poll(1, TimeUnit.SECONDS);
                    if (entry != null) {
                        processLogEntry(entry);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (!config.isFailsafeMode()) {
                        // In non-failsafe mode, log the error but continue processing
                        System.err.println("Error processing log entry: " + e.getMessage());
                    }
                }
            }
        } finally {
            shutdownLatch.countDown();
        }
    }

    /**
     * Processes a single log entry with retry logic.
     *
     * @param entry The log entry to process
     */
    private void processLogEntry(LogEntry entry) {
        try {
            retryStrategy.execute(() -> {
                basicClient.sendLogEntry(entry);
                totalSent.incrementAndGet();
                return null;
            });
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            if (!config.isFailsafeMode()) {
                System.err.println("Failed to send log entry after retries: " + e.getMessage());
            }
        }
    }

    /**
     * Periodic flush operation to process queued entries.
     */
    private void periodicFlush() {
        // This method triggers the workers to process any queued entries
        // The actual processing is done by the worker threads
    }

    @Override
    public void close() {
        if (!running.compareAndSet(true, false)) {
            return; // Already closed
        }

        try {
            // Wait for remaining entries to be processed
            flushSync(Duration.ofSeconds(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Shutdown worker pool
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Clear encryption key cache
        if (encryptor != null) {
            encryptor.clearCache();
        }
        
        // Close basic client
        basicClient.close();
    }
}