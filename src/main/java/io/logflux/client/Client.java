package io.logflux.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.logflux.config.ClientConfig;
import io.logflux.crypto.EncryptionResult;
import io.logflux.crypto.Encryptor;
import io.logflux.models.LogEntry;
import io.logflux.models.LogLevel;
import io.logflux.models.LogResponse;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Basic LogFlux client for sending encrypted logs.
 * All log entries are encrypted according to LogFlux API standards.
 */
public class Client implements AutoCloseable {
    private final ClientConfig config;
    private final Encryptor encryptor;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    /**
     * Creates a new Client with the specified configuration.
     *
     * @param config The client configuration
     */
    public Client(ClientConfig config) {
        this.config = config;
        this.encryptor = new Encryptor(config.getSecret());
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.executor = Executors.newCachedThreadPool();
        
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(config.getTimeout().toMillis()))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getTimeout().toMillis()))
                .build();
        
        this.httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Creates a new Client with the specified parameters.
     *
     * @param serverUrl The LogFlux server URL
     * @param node      The node identifier
     * @param apiKey    The API key
     * @param secret    The encryption secret
     * @return A new Client instance
     */
    public static Client create(String serverUrl, String node, String apiKey, String secret) {
        return new Client(new ClientConfig(serverUrl, node, apiKey, secret));
    }

    /**
     * Creates a new Client from environment variables.
     *
     * @param node   The node identifier
     * @param secret The encryption secret
     * @return A new Client instance
     */
    public static Client createFromEnv(String node, String secret) {
        return new Client(ClientConfig.fromEnvironment(node, secret));
    }

    /**
     * Sends a log message with the specified level.
     *
     * @param message The log message
     * @param level   The log level
     * @return The server response
     * @throws LogFluxException if the request fails
     */
    public LogResponse sendLog(String message, LogLevel level) {
        return sendLog(message, level, Instant.now());
    }

    /**
     * Sends a log message with the specified level and timestamp.
     *
     * @param message   The log message
     * @param level     The log level
     * @param timestamp The timestamp
     * @return The server response
     * @throws LogFluxException if the request fails
     */
    public LogResponse sendLog(String message, LogLevel level, Instant timestamp) {
        // Encrypt the message
        EncryptionResult encryptionResult = encryptor.encryptToResult(message);
        
        // Create log entry with all required fields
        LogEntry entry = new LogEntry(
            config.getNode(),
            encryptionResult.getEncryptedPayload(),
            level,
            timestamp,
            encryptionResult.getEncryptionMode(),
            encryptionResult.getIv(),
            encryptionResult.getSalt()
        );
        
        return sendLogEntry(entry);
    }

    /**
     * Sends multiple log entries in a batch.
     *
     * @param entries The log entries
     * @return The server response
     * @throws LogFluxException if the request fails
     */
    public List<LogResponse> sendLogBatch(List<LogEntry> entries) {
        if (entries == null) {
            throw new LogFluxException("Entries list cannot be null");
        }
        if (entries.isEmpty()) {
            throw new LogFluxException("Entries list cannot be empty");
        }
        
        List<LogResponse> responses = new ArrayList<>();
        for (LogEntry entry : entries) {
            LogResponse response = sendLogEntry(entry);
            responses.add(response);
        }
        return responses;
    }

    /**
     * Sends a log entry asynchronously.
     *
     * @param entry The log entry
     * @return A CompletableFuture containing the server response
     */
    public CompletableFuture<LogResponse> sendLogAsync(LogEntry entry) {
        return CompletableFuture.supplyAsync(() -> sendLogEntry(entry), executor);
    }

    /**
     * Sends a debug level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse debug(String message) {
        return sendLog(message, LogLevel.DEBUG);
    }

    /**
     * Sends an info level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse info(String message) {
        return sendLog(message, LogLevel.INFO);
    }

    /**
     * Sends a notice level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse notice(String message) {
        return sendLog(message, LogLevel.NOTICE);
    }

    /**
     * Sends a warning level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse warning(String message) {
        return sendLog(message, LogLevel.WARNING);
    }

    /**
     * Sends an error level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse error(String message) {
        return sendLog(message, LogLevel.ERROR);
    }

    /**
     * Sends a warning level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse warn(String message) {
        return sendLog(message, LogLevel.WARN);
    }

    /**
     * Sends a fatal level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse fatal(String message) {
        return sendLog(message, LogLevel.FATAL);
    }

    /**
     * Sends a critical level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse critical(String message) {
        return sendLog(message, LogLevel.CRITICAL);
    }

    /**
     * Sends an alert level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse alert(String message) {
        return sendLog(message, LogLevel.ALERT);
    }

    /**
     * Sends an emergency level log message.
     *
     * @param message The log message
     * @return The server response
     */
    public LogResponse emergency(String message) {
        return sendLog(message, LogLevel.EMERGENCY);
    }

    /**
     * Checks the health of the LogFlux service.
     *
     * @return "OK" if the service is healthy
     * @throws LogFluxException if the health check fails
     */
    public String health() {
        try {
            HttpGet request = new HttpGet(config.getServerUrl() + "/health");
            
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";

                if (statusCode == 200) {
                    return responseBody.trim();
                } else {
                    throw new LogFluxException("Health check failed: HTTP " + statusCode + ": " + responseBody);
                }
            });
        } catch (IOException e) {
            throw new LogFluxException("Failed to perform health check", e);
        }
    }

    /**
     * Gets the version information of the LogFlux service.
     *
     * @return Version information as a Map
     * @throws LogFluxException if the version check fails
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> version() {
        try {
            HttpGet request = new HttpGet(config.getServerUrl() + "/version");
            
            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";

                if (statusCode == 200) {
                    return objectMapper.readValue(responseBody, Map.class);
                } else {
                    throw new LogFluxException("Version check failed: HTTP " + statusCode + ": " + responseBody);
                }
            });
        } catch (IOException e) {
            throw new LogFluxException("Failed to get version information", e);
        }
    }

    /**
     * Gets the client configuration.
     *
     * @return The client configuration
     */
    public ClientConfig getConfig() {
        return config;
    }

    /**
     * Gets the encryptor instance.
     *
     * @return The encryptor
     */
    public Encryptor getEncryptor() {
        return encryptor;
    }

    /**
     * Sends a log entry to the server.
     *
     * @param entry The log entry
     * @return The server response
     * @throws LogFluxException if the request fails
     */
    LogResponse sendLogEntry(LogEntry entry) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(entry);
            HttpPost request = new HttpPost(config.getServerUrl() + "/v1/ingest");
            request.setHeader("Authorization", "Bearer " + config.getApiKey());
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

            return httpClient.execute(request, response -> {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();
                String responseBody = entity != null ? EntityUtils.toString(entity) : "";

                if (statusCode == 200 || statusCode == 201) {
                    return objectMapper.readValue(responseBody, LogResponse.class);
                } else {
                    throw new LogFluxException("HTTP " + statusCode + ": " + responseBody);
                }
            });
        } catch (IOException e) {
            throw new LogFluxException("Failed to send log entry", e);
        }
    }

    @Override
    public void close() {
        // Clear encryption key cache
        if (encryptor != null) {
            encryptor.clearCache();
        }
        
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
        
        if (executor != null) {
            executor.shutdown();
        }
    }
}