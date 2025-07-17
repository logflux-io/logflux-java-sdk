package io.logflux.client;

/**
 * Exception thrown by LogFlux client operations.
 */
public class LogFluxException extends RuntimeException {
    /**
     * Creates a new LogFluxException with the specified message.
     *
     * @param message The error message
     */
    public LogFluxException(String message) {
        super(message);
    }

    /**
     * Creates a new LogFluxException with the specified message and cause.
     *
     * @param message The error message
     * @param cause   The underlying cause
     */
    public LogFluxException(String message, Throwable cause) {
        super(message, cause);
    }
}