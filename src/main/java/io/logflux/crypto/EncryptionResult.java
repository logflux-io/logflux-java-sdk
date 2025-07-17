package io.logflux.crypto;

import java.util.Objects;

/**
 * Represents the result of an encryption operation.
 * Contains the encrypted payload and the cryptographic parameters needed for decryption.
 */
public class EncryptionResult {
    private final String encryptedPayload;
    private final String iv;
    private final String salt;
    private final int encryptionMode;

    /**
     * Creates a new EncryptionResult.
     *
     * @param encryptedPayload The base64-encoded encrypted data (without IV or salt)
     * @param iv              The base64-encoded initialization vector (12 bytes)
     * @param salt            The base64-encoded salt (32 bytes)
     * @param encryptionMode  The encryption mode used (1-4)
     */
    public EncryptionResult(String encryptedPayload, String iv, String salt, int encryptionMode) {
        this.encryptedPayload = Objects.requireNonNull(encryptedPayload, "Encrypted payload cannot be null");
        this.iv = Objects.requireNonNull(iv, "IV cannot be null");
        this.salt = Objects.requireNonNull(salt, "Salt cannot be null");
        this.encryptionMode = encryptionMode;
    }

    /**
     * Gets the encrypted payload.
     *
     * @return The base64-encoded encrypted data
     */
    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    /**
     * Gets the initialization vector.
     *
     * @return The base64-encoded IV
     */
    public String getIv() {
        return iv;
    }

    /**
     * Gets the salt.
     *
     * @return The base64-encoded salt
     */
    public String getSalt() {
        return salt;
    }

    /**
     * Gets the encryption mode.
     *
     * @return The encryption mode (1-4)
     */
    public int getEncryptionMode() {
        return encryptionMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptionResult that = (EncryptionResult) o;
        return encryptionMode == that.encryptionMode &&
                Objects.equals(encryptedPayload, that.encryptedPayload) &&
                Objects.equals(iv, that.iv) &&
                Objects.equals(salt, that.salt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encryptedPayload, iv, salt, encryptionMode);
    }

    @Override
    public String toString() {
        return "EncryptionResult{" +
                "encryptionMode=" + encryptionMode +
                ", payloadLength=" + encryptedPayload.length() +
                ", ivLength=" + iv.length() +
                ", saltLength=" + salt.length() +
                '}';
    }
}