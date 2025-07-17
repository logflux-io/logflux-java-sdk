package io.logflux.crypto;

/**
 * Exception thrown when encryption or decryption operations fail.
 */
public class EncryptionException extends RuntimeException {
    /**
     * Creates a new EncryptionException with the specified message.
     *
     * @param message The error message
     */
    public EncryptionException(String message) {
        super(message);
    }

    /**
     * Creates a new EncryptionException with the specified message and cause.
     *
     * @param message The error message
     * @param cause   The underlying cause
     */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}