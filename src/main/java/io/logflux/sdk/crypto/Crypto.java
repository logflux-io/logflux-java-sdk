package io.logflux.sdk.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * AES-256-GCM encryption, gzip compression, and RSA-OAEP key exchange.
 * Uses only Java stdlib (javax.crypto, java.security, java.util.zip).
 */
public final class Crypto {

    private static final int KEY_SIZE = 32;
    private static final int NONCE_SIZE = 12;
    private static final int TAG_BITS = 128;
    private static final int MAX_DECOMPRESS_SIZE = 10 * 1024 * 1024; // 10 MiB
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Object lock = new Object();
    private byte[] aesKey;

    /**
     * Creates a new Crypto instance with the given AES-256 key.
     * Makes a defensive copy.
     */
    public Crypto(byte[] aesKey) {
        if (aesKey == null || aesKey.length != KEY_SIZE) {
            throw new IllegalArgumentException("AES key must be " + KEY_SIZE + " bytes");
        }
        this.aesKey = Arrays.copyOf(aesKey, aesKey.length);
    }

    /**
     * Zeros the AES key material. Must not use this instance after close.
     */
    public void close() {
        synchronized (lock) {
            if (aesKey != null) {
                Arrays.fill(aesKey, (byte) 0);
                aesKey = null;
            }
        }
    }

    /**
     * Result of raw encryption (for multipart/mixed transport).
     */
    public static final class RawResult {
        public final byte[] ciphertext;
        public final byte[] nonce;

        public RawResult(byte[] ciphertext, byte[] nonce) {
            this.ciphertext = ciphertext;
            this.nonce = nonce;
        }
    }

    /**
     * Encrypts plaintext and returns raw bytes.
     */
    public RawResult encryptRaw(byte[] plaintext, boolean compress) throws Exception {
        byte[] data = plaintext;
        if (compress) {
            data = gzipCompress(data);
        }

        byte[] keyCopy;
        synchronized (lock) {
            if (aesKey == null) throw new IllegalStateException("Crypto is closed");
            keyCopy = Arrays.copyOf(aesKey, aesKey.length);
        }

        try {
            byte[] nonce = new byte[NONCE_SIZE];
            RANDOM.nextBytes(nonce);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyCopy, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BITS, nonce);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(data);
            return new RawResult(ciphertext, nonce);
        } finally {
            Arrays.fill(keyCopy, (byte) 0);
        }
    }

    /**
     * Decrypts ciphertext with the given nonce.
     */
    public byte[] decryptRaw(byte[] ciphertext, byte[] nonce, boolean decompress) throws Exception {
        byte[] keyCopy;
        synchronized (lock) {
            if (aesKey == null) throw new IllegalStateException("Crypto is closed");
            keyCopy = Arrays.copyOf(aesKey, aesKey.length);
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyCopy, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_BITS, nonce);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            if (decompress) {
                plaintext = gzipDecompress(plaintext);
            }
            return plaintext;
        } finally {
            Arrays.fill(keyCopy, (byte) 0);
        }
    }

    // --- Static utility methods ---

    /**
     * Generates a random 32-byte AES-256 key.
     */
    public static byte[] generateAESKey() {
        byte[] key = new byte[KEY_SIZE];
        RANDOM.nextBytes(key);
        return key;
    }

    /**
     * Compresses data with gzip.
     */
    public static byte[] gzipCompress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    /**
     * Decompresses gzip data with size limit.
     */
    public static byte[] gzipDecompress(byte[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buf = new byte[4096];
            int total = 0;
            int n;
            while ((n = gzip.read(buf)) != -1) {
                total += n;
                if (total > MAX_DECOMPRESS_SIZE) {
                    throw new RuntimeException("Decompressed data exceeds limit");
                }
                baos.write(buf, 0, n);
            }
        }
        return baos.toByteArray();
    }

    /**
     * Parses a PEM-encoded RSA public key.
     */
    public static PublicKey parseRSAPublicKey(String pem) throws Exception {
        String stripped = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(stripped);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }

    /**
     * Encrypts data with an RSA public key using OAEP-SHA256 padding.
     * Returns base64-encoded ciphertext.
     */
    public static String encryptWithRSA(PublicKey publicKey, byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(data);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * Generates a SHA-256 fingerprint of a public key in DER format.
     * Returns "SHA256:hexstring".
     */
    public static String publicKeyFingerprint(PublicKey publicKey) throws Exception {
        byte[] encoded = publicKey.getEncoded();
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(encoded);
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return "SHA256:" + hex.toString();
    }
}
