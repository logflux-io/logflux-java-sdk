package io.logflux.logger;

import io.logflux.client.ResilientClient;
import io.logflux.config.ResilientClientConfig;
import io.logflux.models.LogLevel;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Global LogFlux logger interface for simple logging.
 */
public class LogFlux {
    private static volatile ResilientClient globalClient;
    private static final Object lock = new Object();

    /**
     * Initializes the global LogFlux client.
     *
     * @param serverUrl The LogFlux server URL
     * @param node      The node identifier
     * @param apiKey    The API key
     * @param secret    The encryption secret
     */
    public static void init(String serverUrl, String node, String apiKey, String secret) {
        synchronized (lock) {
            if (globalClient != null) {
                globalClient.close();
            }
            globalClient = ResilientClient.create(serverUrl, node, apiKey, secret);
        }
    }

    /**
     * Initializes the global LogFlux client with a custom configuration.
     *
     * @param config The client configuration
     */
    public static void init(ResilientClientConfig config) {
        synchronized (lock) {
            if (globalClient != null) {
                globalClient.close();
            }
            globalClient = new ResilientClient(config);
        }
    }

    /**
     * Initializes the global LogFlux client from environment variables.
     *
     * @param node   The node identifier
     * @param secret The encryption secret
     */
    public static void initFromEnv(String node, String secret) {
        synchronized (lock) {
            if (globalClient != null) {
                globalClient.close();
            }
            globalClient = ResilientClient.createFromEnv(node, secret);
        }
    }

    /**
     * Gets the global client instance.
     *
     * @return The global client
     * @throws IllegalStateException if LogFlux is not initialized
     */
    public static ResilientClient getClient() {
        ResilientClient client = globalClient;
        if (client == null) {
            throw new IllegalStateException("LogFlux not initialized. Call init() first.");
        }
        return client;
    }

    /**
     * Sends a debug level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> debug(String message) {
        return getClient().debug(message);
    }

    /**
     * Sends an info level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> info(String message) {
        return getClient().info(message);
    }

    /**
     * Sends a warning level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> warn(String message) {
        return getClient().warn(message);
    }

    /**
     * Sends an error level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> error(String message) {
        return getClient().error(message);
    }

    /**
     * Sends a fatal level log message.
     *
     * @param message The log message
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> fatal(String message) {
        return getClient().fatal(message);
    }

    /**
     * Sends a log message with the specified level.
     *
     * @param message The log message
     * @param level   The log level
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> log(String message, LogLevel level) {
        return getClient().sendLog(message, level);
    }

    /**
     * Sends a log message with the specified level and timestamp.
     *
     * @param message   The log message
     * @param level     The log level
     * @param timestamp The timestamp
     * @return A CompletableFuture that completes when the message is queued
     */
    public static CompletableFuture<Void> log(String message, LogLevel level, Instant timestamp) {
        return getClient().sendLog(message, level, timestamp);
    }

    /**
     * Flushes all queued log entries.
     *
     * @return A CompletableFuture that completes when all entries are flushed
     */
    public static CompletableFuture<Void> flush() {
        return getClient().flush(java.time.Duration.ofSeconds(10));
    }

    /**
     * Closes the global LogFlux client.
     */
    public static void close() {
        synchronized (lock) {
            if (globalClient != null) {
                globalClient.close();
                globalClient = null;
            }
        }
    }

    /**
     * Checks if LogFlux is initialized.
     *
     * @return true if LogFlux is initialized
     */
    public static boolean isInitialized() {
        return globalClient != null;
    }
}