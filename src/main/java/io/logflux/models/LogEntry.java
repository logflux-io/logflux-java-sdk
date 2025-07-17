package io.logflux.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.logflux.utils.CustomInstantDeserializer;
import io.logflux.utils.CustomInstantSerializer;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a log entry to be sent to LogFlux.
 * All log entries must be encrypted as per LogFlux API standards.
 */
public class LogEntry {
    @JsonProperty("node")
    private final String node;

    @JsonProperty("payload")
    private final String payload;

    @JsonProperty("loglevel")
    private final int logLevel;

    @JsonProperty("timestamp")
    @JsonSerialize(using = CustomInstantSerializer.class)
    @JsonDeserialize(using = CustomInstantDeserializer.class)
    private final Instant timestamp;

    @JsonProperty("encryption_mode")
    private final int encryptionMode;

    @JsonProperty("iv")
    private final String iv;

    @JsonProperty("salt")
    private final String salt;

    /**
     * Creates a new LogEntry with all required fields.
     *
     * @param node           The node identifier (max 255 chars)
     * @param payload        The encrypted log data (base64 encoded, max 1MB)
     * @param logLevel       The log level (1-8)
     * @param timestamp      The timestamp
     * @param encryptionMode The encryption mode (1-4)
     * @param iv             The initialization vector (base64 encoded, 12 bytes)
     * @param salt           The salt for key derivation (base64 encoded, 32 bytes)
     */
    public LogEntry(String node, String payload, LogLevel logLevel, Instant timestamp, 
                    int encryptionMode, String iv, String salt) {
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.logLevel = Objects.requireNonNull(logLevel, "LogLevel cannot be null").getValue();
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.encryptionMode = encryptionMode;
        this.iv = Objects.requireNonNull(iv, "IV cannot be null");
        this.salt = Objects.requireNonNull(salt, "Salt cannot be null");
        
        validateFields();
    }

    /**
     * Simplified constructor for testing purposes.
     * Uses default values for encryption fields.
     *
     * @param node      The node identifier
     * @param payload   The log data (unencrypted)
     * @param logLevel  The log level
     * @param timestamp The timestamp
     */
    public LogEntry(String node, String payload, LogLevel logLevel, Instant timestamp) {
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.logLevel = Objects.requireNonNull(logLevel, "LogLevel cannot be null").getValue();
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.encryptionMode = 1; // Default encryption mode
        this.iv = "dGVzdGl2"; // Base64 encoded "testiv"
        this.salt = "dGVzdHNhbHQ="; // Base64 encoded "testsalt"
        
        validateFields();
    }

    /**
     * Constructor for JSON/testing that takes int logLevel directly.
     *
     * @param node      The node identifier
     * @param payload   The log data
     * @param logLevel  The log level as int
     * @param timestamp The timestamp
     */
    public LogEntry(String node, String payload, int logLevel, Instant timestamp) {
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.logLevel = logLevel;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.encryptionMode = 1; // Default encryption mode
        this.iv = "dGVzdGl2"; // Base64 encoded "testiv"
        this.salt = "dGVzdHNhbHQ="; // Base64 encoded "testsalt"
        
        validateFields();
    }

    /**
     * Jackson deserialization constructor.
     */
    @JsonCreator
    public LogEntry(
            @JsonProperty("node") String node,
            @JsonProperty("payload") String payload,
            @JsonProperty("loglevel") int logLevel,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("encryption_mode") int encryptionMode,
            @JsonProperty("iv") String iv,
            @JsonProperty("salt") String salt) {
        this.node = Objects.requireNonNull(node, "Node cannot be null");
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.logLevel = logLevel;
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.encryptionMode = encryptionMode != 0 ? encryptionMode : 1; // Default to mode 1 if not specified
        this.iv = iv; // Allow null for deserialization
        this.salt = salt; // Allow null for deserialization
        
        validateFields();
    }

    private void validateFields() {
        if (node.length() > 255) {
            throw new IllegalArgumentException("Node field exceeds maximum length of 255 characters");
        }
        
        if (logLevel < 0 || logLevel > 4) {
            throw new IllegalArgumentException("Log level must be between 0 and 4");
        }
        
        if (encryptionMode < 1 || encryptionMode > 4) {
            throw new IllegalArgumentException("Encryption mode must be between 1 and 4");
        }
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
     * Gets the encrypted payload.
     *
     * @return The encrypted payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Gets the log level value.
     *
     * @return The log level value (1-8)
     */
    public int getLogLevel() {
        return logLevel;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the encryption mode.
     *
     * @return The encryption mode (1-4)
     */
    public int getEncryptionMode() {
        return encryptionMode;
    }

    /**
     * Gets the initialization vector.
     *
     * @return The IV (base64 encoded)
     */
    public String getIv() {
        return iv;
    }

    /**
     * Gets the salt.
     *
     * @return The salt (base64 encoded)
     */
    public String getSalt() {
        return salt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return logLevel == logEntry.logLevel &&
                encryptionMode == logEntry.encryptionMode &&
                Objects.equals(node, logEntry.node) &&
                Objects.equals(payload, logEntry.payload) &&
                Objects.equals(timestamp, logEntry.timestamp) &&
                Objects.equals(iv, logEntry.iv) &&
                Objects.equals(salt, logEntry.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, payload, logLevel, timestamp, encryptionMode, iv, salt);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "node='" + node + '\'' +
                ", payload='" + payload + '\'' +
                ", logLevel=" + logLevel +
                ", timestamp=" + timestamp +
                ", encryptionMode=" + encryptionMode +
                ", iv='" + iv + '\'' +
                ", salt='" + salt + '\'' +
                '}';
    }
}