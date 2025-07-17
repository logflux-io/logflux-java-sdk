package io.logflux.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.logflux.utils.CustomInstantDeserializer;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a response from the LogFlux server.
 */
public class LogResponse {
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("status")
    private String status;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("timestamp")
    @JsonDeserialize(using = CustomInstantDeserializer.class)
    private Instant timestamp;

    @JsonProperty("message")
    private String message;

    /**
     * Default constructor for JSON deserialization.
     */
    public LogResponse() {
    }

    /**
     * Creates a new LogResponse with a simple success flag and message.
     * Used for testing and error responses.
     *
     * @param success Whether the operation was successful
     * @param message The response message
     */
    public LogResponse(boolean success, String message) {
        this.success = success;
        this.status = success ? "accepted" : "error";
        this.id = null;
        this.timestamp = Instant.now();
        this.message = message;
    }

    /**
     * Creates a new LogResponse.
     *
     * @param status    The response status
     * @param id        The log entry ID
     * @param timestamp The server timestamp
     * @param message   Optional message
     */
    public LogResponse(String status, Long id, Instant timestamp, String message) {
        this.success = "accepted".equals(status);
        this.status = status;
        this.id = id;
        this.timestamp = timestamp;
        this.message = message;
    }

    /**
     * Gets the success status.
     *
     * @return The success status
     */
    public Boolean getSuccess() {
        return success;
    }

    /**
     * Sets the success status.
     *
     * @param success The success status
     */
    public void setSuccess(Boolean success) {
        this.success = success;
    }

    /**
     * Gets the response status.
     *
     * @return The status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the response status.
     *
     * @param status The status
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the log entry ID.
     *
     * @return The ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the log entry ID.
     *
     * @param id The ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the server timestamp.
     *
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the server timestamp.
     *
     * @param timestamp The timestamp
     */
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the optional message.
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the optional message.
     *
     * @param message The message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Checks if the response indicates success.
     *
     * @return true if success is true or status is "accepted"
     */
    public boolean isSuccess() {
        return Boolean.TRUE.equals(success) || "accepted".equals(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogResponse that = (LogResponse) o;
        return Objects.equals(success, that.success) &&
                Objects.equals(status, that.status) &&
                Objects.equals(id, that.id) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, status, id, timestamp, message);
    }

    @Override
    public String toString() {
        return "LogResponse{" +
                "success=" + success +
                ", status='" + status + '\'' +
                ", id=" + id +
                ", timestamp=" + timestamp +
                ", message='" + message + '\'' +
                '}';
    }
}