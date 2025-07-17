package io.logflux.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for resilient LogFlux clients.
 */
public class ResilientClientConfig {
    private final String serverUrl;
    private final String node;
    private final String apiKey;
    private final String secret;
    private final Duration timeout;
    private final int queueSize;
    private final Duration flushInterval;
    private final int workerCount;
    private final boolean failsafeMode;
    private final int maxRetries;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double backoffFactor;
    private final boolean jitterEnabled;

    /**
     * Creates a new ResilientClientConfig.
     */
    public ResilientClientConfig(String serverUrl, String node, String apiKey, String secret,
                                Duration timeout, int queueSize, Duration flushInterval,
                                int workerCount, boolean failsafeMode, int maxRetries,
                                Duration initialDelay, Duration maxDelay, double backoffFactor,
                                boolean jitterEnabled) {
        this.serverUrl = Objects.requireNonNull(serverUrl, "Server URL cannot be null");
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.secret = Objects.requireNonNull(secret, "Secret cannot be null");
        this.timeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
        this.flushInterval = Objects.requireNonNull(flushInterval, "Flush interval cannot be null");
        this.initialDelay = Objects.requireNonNull(initialDelay, "Initial delay cannot be null");
        this.maxDelay = Objects.requireNonNull(maxDelay, "Max delay cannot be null");
        
        this.queueSize = queueSize;
        this.workerCount = workerCount;
        this.failsafeMode = failsafeMode;
        this.maxRetries = maxRetries;
        this.backoffFactor = backoffFactor;
        this.jitterEnabled = jitterEnabled;
        
        validateConfig();
    }

    /**
     * Creates a ResilientClientConfig from environment variables.
     *
     * @param node   The node identifier
     * @param secret The encryption secret
     * @return A new ResilientClientConfig
     */
    public static ResilientClientConfig fromEnvironment(String node, String secret) {
        String serverUrl = System.getenv("LOGFLUX_SERVER_URL");
        String apiKey = System.getenv("LOGFLUX_API_KEY");
        
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("LOGFLUX_SERVER_URL environment variable is required");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("LOGFLUX_API_KEY environment variable is required");
        }
        
        return new Builder()
                .serverUrl(serverUrl.trim())
                .node(node)
                .apiKey(apiKey.trim())
                .secret(secret)
                .timeout(getEnvDuration("LOGFLUX_HTTP_TIMEOUT", Duration.ofSeconds(30)))
                .queueSize(getEnvInt("LOGFLUX_QUEUE_SIZE", 1000))
                .flushInterval(getEnvDuration("LOGFLUX_FLUSH_INTERVAL", Duration.ofSeconds(5)))
                .workerCount(getEnvInt("LOGFLUX_WORKER_COUNT", 2))
                .failsafeMode(getEnvBoolean("LOGFLUX_FAILSAFE_MODE", true))
                .maxRetries(getEnvInt("LOGFLUX_MAX_RETRIES", 5))
                .initialDelay(getEnvDuration("LOGFLUX_INITIAL_DELAY", Duration.ofMillis(100)))
                .maxDelay(getEnvDuration("LOGFLUX_MAX_DELAY", Duration.ofSeconds(30)))
                .backoffFactor(getEnvDouble("LOGFLUX_BACKOFF_FACTOR", 2.0))
                .jitterEnabled(getEnvBoolean("LOGFLUX_JITTER_ENABLED", true))
                .build();
    }

    /**
     * Creates a default ResilientClientConfig.
     *
     * @param serverUrl The server URL
     * @param node      The node identifier
     * @param apiKey    The API key
     * @param secret    The encryption secret
     * @return A new ResilientClientConfig with default values
     */
    public static ResilientClientConfig defaultConfig(String serverUrl, String node, String apiKey, String secret) {
        return new Builder()
                .serverUrl(serverUrl)
                .node(node)
                .apiKey(apiKey)
                .secret(secret)
                .build();
    }

    private void validateConfig() {
        if (serverUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Server URL cannot be empty");
        }
        
        if (node.trim().isEmpty()) {
            throw new IllegalArgumentException("Node cannot be empty");
        }
        
        if (apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be empty");
        }
        
        if (secret.trim().isEmpty()) {
            throw new IllegalArgumentException("Secret cannot be empty");
        }
        
        if (!apiKey.startsWith("lf_")) {
            throw new IllegalArgumentException("API key must start with 'lf_'");
        }
        
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Server URL must start with 'http://' or 'https://'");
        }
        
        if (queueSize <= 0) {
            throw new IllegalArgumentException("Queue size must be positive");
        }
        
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Worker count must be positive");
        }
        
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries cannot be negative");
        }
        
        if (backoffFactor < 1.0) {
            throw new IllegalArgumentException("Backoff factor must be >= 1.0");
        }
        
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        
        if (flushInterval.isNegative()) {
            throw new IllegalArgumentException("Flush interval cannot be negative");
        }
        
        if (initialDelay.isNegative()) {
            throw new IllegalArgumentException("Initial delay cannot be negative");
        }
        
        if (maxDelay.isNegative()) {
            throw new IllegalArgumentException("Max delay cannot be negative");
        }
    }

    // Getters
    public String getServerUrl() { return serverUrl; }
    public String getNode() { return node; }
    public String getApiKey() { return apiKey; }
    public String getSecret() { return secret; }
    public Duration getTimeout() { return timeout; }
    public int getQueueSize() { return queueSize; }
    public Duration getFlushInterval() { return flushInterval; }
    public int getWorkerCount() { return workerCount; }
    public boolean isFailsafeMode() { return failsafeMode; }
    public int getMaxRetries() { return maxRetries; }
    public Duration getInitialDelay() { return initialDelay; }
    public Duration getMaxDelay() { return maxDelay; }
    public double getBackoffFactor() { return backoffFactor; }
    public boolean isJitterEnabled() { return jitterEnabled; }

    /**
     * Converts this config to a basic ClientConfig.
     *
     * @return A ClientConfig with basic settings
     */
    public ClientConfig toClientConfig() {
        return new ClientConfig(serverUrl, node, apiKey, secret, timeout);
    }

    // Helper methods for environment variable parsing
    private static Duration getEnvDuration(String name, Duration defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int getEnvInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean getEnvBoolean(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private static double getEnvDouble(String name, double defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Builder class for ResilientClientConfig.
     */
    public static class Builder {
        private String serverUrl;
        private String node;
        private String apiKey;
        private String secret;
        private Duration timeout = Duration.ofSeconds(30);
        private int queueSize = 1000;
        private Duration flushInterval = Duration.ofSeconds(5);
        private int workerCount = 2;
        private boolean failsafeMode = true;
        private int maxRetries = 5;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(30);
        private double backoffFactor = 2.0;
        private boolean jitterEnabled = true;

        public Builder serverUrl(String serverUrl) { this.serverUrl = serverUrl; return this; }
        public Builder node(String node) { this.node = node; return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder timeout(Duration timeout) { this.timeout = timeout; return this; }
        public Builder queueSize(int queueSize) { this.queueSize = queueSize; return this; }
        public Builder flushInterval(Duration flushInterval) { this.flushInterval = flushInterval; return this; }
        public Builder workerCount(int workerCount) { this.workerCount = workerCount; return this; }
        public Builder failsafeMode(boolean failsafeMode) { this.failsafeMode = failsafeMode; return this; }
        public Builder maxRetries(int maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder initialDelay(Duration initialDelay) { this.initialDelay = initialDelay; return this; }
        public Builder maxDelay(Duration maxDelay) { this.maxDelay = maxDelay; return this; }
        public Builder backoffFactor(double backoffFactor) { this.backoffFactor = backoffFactor; return this; }
        public Builder jitterEnabled(boolean jitterEnabled) { this.jitterEnabled = jitterEnabled; return this; }

        public ResilientClientConfig build() {
            return new ResilientClientConfig(serverUrl, node, apiKey, secret, timeout, queueSize,
                    flushInterval, workerCount, failsafeMode, maxRetries, initialDelay, maxDelay,
                    backoffFactor, jitterEnabled);
        }
    }
}