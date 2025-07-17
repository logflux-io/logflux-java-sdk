package io.logflux.config;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for LogFlux clients.
 */
public class ClientConfig {
    private final String serverUrl;
    private final String node;
    private final String apiKey;
    private final String secret;
    private final Duration timeout;

    /**
     * Creates a new ClientConfig.
     *
     * @param serverUrl The LogFlux server URL
     * @param node      The node identifier
     * @param apiKey    The API key
     * @param secret    The encryption secret
     * @param timeout   The HTTP timeout
     */
    public ClientConfig(String serverUrl, String node, String apiKey, String secret, Duration timeout) {
        this.serverUrl = Objects.requireNonNull(serverUrl, "Server URL cannot be null");
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.apiKey = Objects.requireNonNull(apiKey, "API key cannot be null");
        this.secret = Objects.requireNonNull(secret, "Secret cannot be null");
        this.timeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
        
        validateConfig();
    }

    /**
     * Creates a new ClientConfig with default timeout.
     *
     * @param serverUrl The LogFlux server URL
     * @param node      The node identifier
     * @param apiKey    The API key
     * @param secret    The encryption secret
     */
    public ClientConfig(String serverUrl, String node, String apiKey, String secret) {
        this(serverUrl, node, apiKey, secret, Duration.ofSeconds(30));
    }

    /**
     * Creates a ClientConfig from environment variables.
     *
     * @param node   The node identifier
     * @param secret The encryption secret
     * @return A new ClientConfig
     * @throws IllegalArgumentException if required environment variables are missing
     */
    public static ClientConfig fromEnvironment(String node, String secret) {
        String serverUrl = System.getenv("LOGFLUX_SERVER_URL");
        String apiKey = System.getenv("LOGFLUX_API_KEY");
        
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("LOGFLUX_SERVER_URL environment variable is required");
        }
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("LOGFLUX_API_KEY environment variable is required");
        }
        
        Duration timeout = Duration.ofSeconds(30);
        String timeoutStr = System.getenv("LOGFLUX_HTTP_TIMEOUT");
        if (timeoutStr != null && !timeoutStr.trim().isEmpty()) {
            try {
                timeout = Duration.ofSeconds(Long.parseLong(timeoutStr));
            } catch (NumberFormatException e) {
                // Use default timeout if parsing fails
            }
        }
        
        return new ClientConfig(serverUrl.trim(), node, apiKey.trim(), secret, timeout);
    }

    /**
     * Validates the configuration.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
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
        
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
    }

    /**
     * Gets the server URL.
     *
     * @return The server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Gets the node identifier.
     *
     * @return The node identifier
     */
    public String getNode() {
        return node;
    }

    /**
     * Gets the API key.
     *
     * @return The API key
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Gets the encryption secret.
     *
     * @return The encryption secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Gets the HTTP timeout.
     *
     * @return The timeout duration
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Creates a new ClientConfig with a different timeout.
     *
     * @param timeout The new timeout
     * @return A new ClientConfig with the specified timeout
     */
    public ClientConfig withTimeout(Duration timeout) {
        return new ClientConfig(serverUrl, node, apiKey, secret, timeout);
    }

    /**
     * Creates a new ClientConfig with a different server URL.
     *
     * @param serverUrl The new server URL
     * @return A new ClientConfig with the specified server URL
     */
    public ClientConfig withServerUrl(String serverUrl) {
        return new ClientConfig(serverUrl, node, apiKey, secret, timeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientConfig that = (ClientConfig) o;
        return Objects.equals(serverUrl, that.serverUrl) &&
                Objects.equals(node, that.node) &&
                Objects.equals(apiKey, that.apiKey) &&
                Objects.equals(secret, that.secret) &&
                Objects.equals(timeout, that.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverUrl, node, apiKey, secret, timeout);
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "serverUrl='" + serverUrl + '\'' +
                ", node='" + node + '\'' +
                ", apiKey='" + maskApiKey(apiKey) + '\'' +
                ", secret='***'" +
                ", timeout=" + timeout +
                '}';
    }

    /**
     * Masks the API key for logging purposes.
     *
     * @param apiKey The API key to mask
     * @return The masked API key
     */
    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }
}