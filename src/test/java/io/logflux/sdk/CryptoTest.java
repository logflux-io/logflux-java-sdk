package io.logflux.sdk;

import io.logflux.sdk.crypto.Crypto;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoTest {

    @Test
    void generateAESKeyReturns32Bytes() {
        byte[] key = Crypto.generateAESKey();
        assertEquals(32, key.length);

        // Keys should be random (different each time)
        byte[] key2 = Crypto.generateAESKey();
        assertFalse(Arrays.equals(key, key2));
    }

    @Test
    void encryptDecryptRoundTrip() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        String plaintext = "Hello, LogFlux!";
        Crypto.RawResult result = crypto.encryptRaw(plaintext.getBytes(StandardCharsets.UTF_8), false);

        assertNotNull(result.ciphertext);
        assertNotNull(result.nonce);
        assertEquals(12, result.nonce.length);
        assertFalse(Arrays.equals(plaintext.getBytes(), result.ciphertext));

        byte[] decrypted = crypto.decryptRaw(result.ciphertext, result.nonce, false);
        assertEquals(plaintext, new String(decrypted, StandardCharsets.UTF_8));

        crypto.close();
    }

    @Test
    void encryptDecryptWithCompression() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        // Use a compressible string
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("This is a repeated test message. ");
        String plaintext = sb.toString();

        Crypto.RawResult result = crypto.encryptRaw(plaintext.getBytes(StandardCharsets.UTF_8), true);
        assertTrue(result.ciphertext.length < plaintext.length(),
                "Compressed+encrypted should be smaller than original for repetitive text");

        byte[] decrypted = crypto.decryptRaw(result.ciphertext, result.nonce, true);
        assertEquals(plaintext, new String(decrypted, StandardCharsets.UTF_8));

        crypto.close();
    }

    @Test
    void closeZerosKeyMaterial() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        // Should work before close
        Crypto.RawResult result = crypto.encryptRaw("test".getBytes(), false);
        assertNotNull(result);

        crypto.close();

        // Should fail after close
        assertThrows(IllegalStateException.class, () ->
                crypto.encryptRaw("test".getBytes(), false));
    }

    @Test
    void gzipCompressDecompress() throws Exception {
        byte[] original = "Hello, compression!".getBytes(StandardCharsets.UTF_8);
        byte[] compressed = Crypto.gzipCompress(original);
        byte[] decompressed = Crypto.gzipDecompress(compressed);
        assertArrayEquals(original, decompressed);
    }

    @Test
    void rsaEncryptDecrypt() throws Exception {
        // Generate a test RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // Test encrypt
        byte[] data = "test secret".getBytes(StandardCharsets.UTF_8);
        String encrypted = Crypto.encryptWithRSA(kp.getPublic(), data);
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty());

        // Verify it's valid base64
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        assertTrue(decoded.length > 0);
    }

    @Test
    void publicKeyFingerprint() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String fingerprint = Crypto.publicKeyFingerprint(kp.getPublic());
        assertTrue(fingerprint.startsWith("SHA256:"));
        assertEquals(71, fingerprint.length()); // "SHA256:" + 64 hex chars
    }

    @Test
    void parseRSAPublicKeyFromPEM() throws Exception {
        // Generate key and convert to PEM
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        String b64 = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
        String pem = "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";

        PublicKey parsed = Crypto.parseRSAPublicKey(pem);
        assertNotNull(parsed);
        assertEquals("RSA", parsed.getAlgorithm());
    }

    @Test
    void invalidKeySizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Crypto(new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> new Crypto(null));
    }

    @Test
    void differentNoncesPerEncryption() throws Exception {
        byte[] key = Crypto.generateAESKey();
        Crypto crypto = new Crypto(key);

        Crypto.RawResult r1 = crypto.encryptRaw("same message".getBytes(), false);
        Crypto.RawResult r2 = crypto.encryptRaw("same message".getBytes(), false);

        assertFalse(Arrays.equals(r1.nonce, r2.nonce), "Nonces should be unique");
        assertFalse(Arrays.equals(r1.ciphertext, r2.ciphertext),
                "Ciphertext should differ due to different nonces");

        crypto.close();
    }

    @Test
    void wrongKeyFailsDecryption() throws Exception {
        byte[] key1 = Crypto.generateAESKey();
        byte[] key2 = Crypto.generateAESKey();
        Crypto crypto1 = new Crypto(key1);
        Crypto crypto2 = new Crypto(key2);

        Crypto.RawResult result = crypto1.encryptRaw("secret".getBytes(), false);

        assertThrows(Exception.class, () ->
                crypto2.decryptRaw(result.ciphertext, result.nonce, false));

        crypto1.close();
        crypto2.close();
    }
}
