package io.logflux.models;

/**
 * Represents the encryption modes supported by LogFlux.
 */
public enum EncryptionMode {
    /**
     * AES-256-GCM with PBKDF2 key derivation (600k iterations) - Current Standard
     */
    AES256_GCM_PBKDF2_SHA256_600K(1, "AES256-GCM_PBKDF2-SHA256-600K"),
    
    /**
     * AES-256-GCM with scrypt key derivation - Future
     */
    AES256_GCM_SCRYPT(2, "AES256-GCM_SCRYPT"),
    
    /**
     * AES-256-GCM with Argon2 key derivation - Future
     */
    AES256_GCM_ARGON2(3, "AES256-GCM_ARGON2"),
    
    /**
     * ChaCha20-Poly1305 with Argon2 key derivation - Future
     */
    CHACHA20_POLY1305_ARGON2(4, "ChaCha20-Poly1305_ARGON2");

    private final int value;
    private final String name;

    EncryptionMode(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Gets the numeric value of the encryption mode.
     *
     * @return The numeric value (1-4)
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets the string name of the encryption mode.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Creates an EncryptionMode from its numeric value.
     *
     * @param value The numeric value (1-4)
     * @return The corresponding EncryptionMode
     * @throws IllegalArgumentException if the value is not valid
     */
    public static EncryptionMode fromValue(int value) {
        for (EncryptionMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid encryption mode value: " + value + ". Valid values are 1-4.");
    }

    /**
     * Creates an EncryptionMode from its string name.
     *
     * @param name The name
     * @return The corresponding EncryptionMode
     * @throws IllegalArgumentException if the name is not valid
     */
    public static EncryptionMode fromName(String name) {
        for (EncryptionMode mode : values()) {
            if (mode.name.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid encryption mode name: " + name);
    }

    @Override
    public String toString() {
        return name;
    }
}