package io.logflux.crypto;

import io.logflux.models.EncryptionMode;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles encryption and decryption for log messages according to LogFlux API standards.
 * Supports multiple encryption modes with key caching for performance.
 */
public class Encryptor {
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    private static final int KEY_LENGTH = 32; // 256 bits
    private static final int SALT_LENGTH = 32; // 256 bits (exceeds OWASP minimum)
    
    // PBKDF2 parameters
    private static final int PBKDF2_600K_ITERATIONS = 600000; // OWASP 2023+ recommendation
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    
    private final String secret;
    private final SecureRandom random;
    private final Map<String, SecretKey> keyCache;
    private final EncryptionMode defaultMode;

    /**
     * Creates a new Encryptor with the given secret using the default encryption mode.
     *
     * @param secret The secret string to derive the key from
     * @throws EncryptionException if the secret is invalid
     */
    public Encryptor(String secret) {
        this(secret, EncryptionMode.AES256_GCM_PBKDF2_SHA256_600K);
    }

    /**
     * Creates a new Encryptor with the given secret and encryption mode.
     *
     * @param secret The secret string to derive the key from
     * @param defaultMode The default encryption mode to use
     * @throws EncryptionException if the secret is invalid
     */
    public Encryptor(String secret, EncryptionMode defaultMode) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new EncryptionException("Secret cannot be null or empty");
        }
        
        this.secret = secret;
        this.random = new SecureRandom();
        this.keyCache = new ConcurrentHashMap<>();
        this.defaultMode = defaultMode;
    }

    /**
     * Encrypts a message and returns a Base64-encoded string.
     * This is a convenience method for testing and simple use cases.
     *
     * @param message The message to encrypt
     * @return Base64-encoded encrypted data
     * @throws EncryptionException if encryption fails
     */
    public String encrypt(String message) {
        EncryptionResult result = encryptToResult(message, defaultMode);
        // Combine all components into a single Base64 string
        // Format: [encrypted_payload].[iv].[salt].[mode]
        String combined = result.getEncryptedPayload() + "." + result.getIv() + "." + result.getSalt() + "." + result.getEncryptionMode();
        return Base64.getEncoder().encodeToString(combined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypts a Base64-encoded string created by the encrypt(String) method.
     *
     * @param encryptedData Base64-encoded encrypted data
     * @return The decrypted message
     * @throws EncryptionException if decryption fails
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            throw new EncryptionException("Encrypted data cannot be null or empty");
        }

        try {
            // Decode the Base64 string first
            String combined = new String(Base64.getDecoder().decode(encryptedData), StandardCharsets.UTF_8);
            // Split the dotted format: [encrypted_payload].[iv].[salt].[mode]
            String[] parts = combined.split("\\.");
            
            if (parts.length != 4) {
                throw new EncryptionException("Invalid encrypted data format");
            }

            String payload = parts[0];
            String iv = parts[1];
            String salt = parts[2];
            int mode = Integer.parseInt(parts[3]);

            return decryptFromComponents(payload, iv, salt, mode);
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt data: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypts a message and returns the full EncryptionResult object.
     *
     * @param message The message to encrypt
     * @return The encryption result containing encrypted payload, IV, salt, and mode
     * @throws EncryptionException if encryption fails
     */
    public EncryptionResult encryptToResult(String message) {
        return encryptToResult(message, defaultMode);
    }

    /**
     * Encrypts a message using the specified encryption mode.
     *
     * @param message The message to encrypt
     * @param mode The encryption mode to use
     * @return The encryption result containing encrypted payload, IV, salt, and mode
     * @throws EncryptionException if encryption fails
     */
    public EncryptionResult encryptToResult(String message, EncryptionMode mode) {
        if (message == null) {
            throw new EncryptionException("Message cannot be null");
        }

        try {
            // Generate fresh salt and IV for each encryption
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[GCM_IV_LENGTH];
            random.nextBytes(salt);
            random.nextBytes(iv);

            // Derive key (with caching)
            SecretKey key = deriveKey(secret, salt, mode);

            // Encrypt using AES-GCM
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] encrypted = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Return separate components as per API standards
            return new EncryptionResult(
                Base64.getEncoder().encodeToString(encrypted),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(salt),
                mode.getValue()
            );
        } catch (Exception e) {
            throw new EncryptionException("Failed to encrypt message", e);
        }
    }

        /**
     * Decrypts an encrypted message using the provided cryptographic parameters.
     *
     * @param encryptedPayload The base64-encoded encrypted payload
     * @param iv               The base64-encoded initialization vector
     * @param salt             The base64-encoded salt
     * @param encryptionMode   The encryption mode (1-4)
     * @return The decrypted message
     * @throws EncryptionException if decryption fails
     */
    public String decryptFromComponents(String encryptedPayload, String iv, String salt, int encryptionMode) {
        if (encryptedPayload == null || iv == null || salt == null) {
            throw new EncryptionException("Encrypted payload, IV, and salt cannot be null");
        }

        try {
            // Decode from base64
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedPayload);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            byte[] saltBytes = Base64.getDecoder().decode(salt);

            // Validate sizes
            if (ivBytes.length != GCM_IV_LENGTH) {
                throw new EncryptionException("Invalid IV length: expected " + GCM_IV_LENGTH + " bytes");
            }
            if (saltBytes.length != SALT_LENGTH) {
                throw new EncryptionException("Invalid salt length: expected " + SALT_LENGTH + " bytes");
            }

            // Get encryption mode
            EncryptionMode mode = EncryptionMode.fromValue(encryptionMode);

            // Derive key using the salt
            SecretKey key = deriveKey(secret, saltBytes, mode);

            // Decrypt using AES-GCM
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] decrypted = cipher.doFinal(encryptedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Failed to decrypt message", e);
        }
    }

    /**
     * Derives a key from the secret and salt using the specified encryption mode.
     * Keys are cached based on secret + mode for performance.
     *
     * @param secret The secret string
     * @param salt The salt bytes
     * @param mode The encryption mode
     * @return The derived secret key
     */
    private SecretKey deriveKey(String secret, byte[] salt, EncryptionMode mode) {
        try {
            // Cache key should include salt since different salts produce different keys
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String cacheKey = secret + ":" + mode.getValue() + ":" + saltBase64;
            
            // Check cache first
            SecretKey cachedKey = keyCache.get(cacheKey);
            if (cachedKey != null) {
                return cachedKey;
            }

            SecretKey derivedKey;
            
            switch (mode) {
                case AES256_GCM_PBKDF2_SHA256_600K:
                    // PBKDF2 with SHA-256, 600k iterations
                    SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
                    KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, PBKDF2_600K_ITERATIONS, KEY_LENGTH * 8);
                    SecretKey pbkdf2Key = factory.generateSecret(spec);
                    derivedKey = new SecretKeySpec(pbkdf2Key.getEncoded(), AES_ALGORITHM);
                    break;
                    
                case AES256_GCM_SCRYPT:
                    // TODO: Implement scrypt key derivation when needed
                    throw new EncryptionException("Scrypt key derivation not yet implemented");
                    
                case AES256_GCM_ARGON2:
                    // TODO: Implement Argon2 key derivation when needed
                    throw new EncryptionException("Argon2 key derivation not yet implemented");
                    
                case CHACHA20_POLY1305_ARGON2:
                    // TODO: Implement ChaCha20-Poly1305 with Argon2 when needed
                    throw new EncryptionException("ChaCha20-Poly1305 not yet implemented");
                    
                default:
                    throw new EncryptionException("Unsupported encryption mode: " + mode);
            }

            // Cache the derived key
            keyCache.put(cacheKey, derivedKey);
            
            return derivedKey;
        } catch (Exception e) {
            throw new EncryptionException("Failed to derive key from secret", e);
        }
    }

    /**
     * Clears the key cache. Should be called when the encryptor is no longer needed.
     */
    public void clearCache() {
        keyCache.clear();
    }

    /**
     * Gets the default encryption mode.
     *
     * @return The default encryption mode
     */
    public EncryptionMode getDefaultMode() {
        return defaultMode;
    }

    /**
     * Generates a random encryption key.
     *
     * @return A random SecretKey
     */
    public static SecretKey generateRandomKey() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[KEY_LENGTH];
        random.nextBytes(keyBytes);
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }
}