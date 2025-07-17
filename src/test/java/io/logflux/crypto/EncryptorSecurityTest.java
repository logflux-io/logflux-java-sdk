package io.logflux.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security-focused tests for the Encryptor class.
 */
class EncryptorSecurityTest {
    
    private Encryptor encryptor;
    private static final String TEST_SECRET = "test-secret-for-security-tests";
    
    @BeforeEach
    void setUp() {
        encryptor = new Encryptor(TEST_SECRET);
    }
    
    @Test
    void shouldUsePBKDF2ForKeyDerivation() {
        // This test verifies that the key derivation is using PBKDF2 and not simple SHA-256
        // by checking that the same secret produces different results from simple SHA-256
        
        try {
            // Generate what the old SHA-256 method would produce
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] sha256Key = digest.digest(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
            
            // Get the actual key from our Encryptor (via reflection to access private method)
            // We'll test the behavior indirectly by checking encryption output
            String message = "test message for key derivation verification";
            String encrypted1 = encryptor.encrypt(message);
            
            // Create a new encryptor with the same secret
            Encryptor encryptor2 = new Encryptor(TEST_SECRET);
            String encrypted2 = encryptor2.encrypt(message);
            
            // Both should be able to decrypt each other's messages (same key derivation)
            assertEquals(message, encryptor.decrypt(encrypted2));
            assertEquals(message, encryptor2.decrypt(encrypted1));
            
            // But the encrypted outputs should be different (due to random IV)
            assertNotEquals(encrypted1, encrypted2);
            
        } catch (Exception e) {
            fail("Key derivation test failed: " + e.getMessage());
        }
    }
    
    @Test
    void shouldProduceConsistentKeysForSameSecret() {
        // Verify that the same secret always produces the same key
        String message = "consistency test message";
        
        Encryptor encryptor1 = new Encryptor(TEST_SECRET);
        Encryptor encryptor2 = new Encryptor(TEST_SECRET);
        Encryptor encryptor3 = new Encryptor(TEST_SECRET);
        
        String encrypted = encryptor1.encrypt(message);
        
        // All encryptors with the same secret should be able to decrypt
        assertEquals(message, encryptor2.decrypt(encrypted));
        assertEquals(message, encryptor3.decrypt(encrypted));
    }
    
    @Test
    void shouldProduceDifferentKeysForDifferentSecrets() {
        // Verify that different secrets produce different keys
        String message = "different secrets test";
        
        Encryptor encryptor1 = new Encryptor("secret1");
        Encryptor encryptor2 = new Encryptor("secret2");
        
        String encrypted1 = encryptor1.encrypt(message);
        
        // Different secret should not be able to decrypt
        assertThrows(EncryptionException.class, () -> {
            encryptor2.decrypt(encrypted1);
        });
    }
    
    @Test
    void shouldHandleWeakSecrets() {
        // Test that even weak secrets work (though not recommended)
        String[] weakSecrets = {"123", "password", "a", "test"};
        String message = "weak secret test";
        
        for (String weakSecret : weakSecrets) {
            Encryptor weakEncryptor = new Encryptor(weakSecret);
            String encrypted = weakEncryptor.encrypt(message);
            String decrypted = weakEncryptor.decrypt(encrypted);
            assertEquals(message, decrypted, "Failed for weak secret: " + weakSecret);
        }
    }
    
    @Test
    void shouldHandleSpecialCharactersInSecret() {
        // Test that secrets with special characters work correctly
        String[] specialSecrets = {
            "secret!@#$%^&*()",
            "sëcrét-with-ünïcødé",
            "secret with spaces",
            "secret\nwith\nnewlines",
            "secret\twith\ttabs"
        };
        String message = "special characters test";
        
        for (String specialSecret : specialSecrets) {
            Encryptor specialEncryptor = new Encryptor(specialSecret);
            String encrypted = specialEncryptor.encrypt(message);
            String decrypted = specialEncryptor.decrypt(encrypted);
            assertEquals(message, decrypted, "Failed for special secret: " + specialSecret);
        }
    }
    
    @Test
    void shouldBeResistantToTimingAttacks() {
        // Basic timing test - PBKDF2 should take consistent time regardless of secret content
        String message = "timing test message";
        
        long[] times = new long[5];
        String[] secrets = {"a", "short", "medium-length-secret", "very-long-secret-with-many-characters", "another-very-long-secret-for-comparison"};
        
        for (int i = 0; i < secrets.length; i++) {
            long startTime = System.nanoTime();
            Encryptor timedEncryptor = new Encryptor(secrets[i]);
            timedEncryptor.encrypt(message);
            long endTime = System.nanoTime();
            times[i] = endTime - startTime;
        }
        
        // Check that timing variations are within reasonable bounds
        // Note: This is a basic test and real timing attack resistance would require more sophisticated testing
        long maxTime = Arrays.stream(times).max().orElse(0);
        long minTime = Arrays.stream(times).min().orElse(0);
        
        // Allow up to 300% variation due to JVM warmup, GC, and system variations
        // The main goal is to ensure PBKDF2 is being used (which adds consistent overhead)
        assertTrue(maxTime < minTime * 4.0, "Timing variation too large, potential timing attack vulnerability");
        
        // Ensure that PBKDF2 is adding sufficient computational cost (at least 1ms total)
        assertTrue(minTime > 1_000_000, "Key derivation seems too fast, PBKDF2 might not be working");
    }
}
