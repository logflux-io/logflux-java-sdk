package io.logflux.crypto;

import io.logflux.util.TestConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Encryptor class.
 */
class EncryptorTest {
    private Encryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new Encryptor(TestConstants.TEST_SECRET);
    }

    @Test
    void shouldEncryptMessage() {
        // Given
        String message = "Hello, LogFlux!";

        // When
        String encrypted = encryptor.encrypt(message);

        // Then
        assertNotNull(encrypted);
        assertNotEquals(message, encrypted);
        assertTrue(encrypted.matches("^[A-Za-z0-9+/]+=*$")); // Base64 pattern
    }

    @Test
    void shouldDecryptMessage() {
        // Given
        String originalMessage = "Hello, LogFlux!";
        String encrypted = encryptor.encrypt(originalMessage);

        // When
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(originalMessage, decrypted);
    }

    @Test
    void shouldEncryptDifferentMessagesDifferently() {
        // Given
        String message1 = "Message 1";
        String message2 = "Message 2";

        // When
        String encrypted1 = encryptor.encrypt(message1);
        String encrypted2 = encryptor.encrypt(message2);

        // Then
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void shouldEncryptSameMessageDifferentlyEachTime() {
        // Given
        String message = "Same message";

        // When
        String encrypted1 = encryptor.encrypt(message);
        String encrypted2 = encryptor.encrypt(message);

        // Then
        assertNotEquals(encrypted1, encrypted2); // Due to different IV
    }

    @Test
    void shouldHandleEmptyMessage() {
        // Given
        String emptyMessage = "";

        // When
        String encrypted = encryptor.encrypt(emptyMessage);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(emptyMessage, decrypted);
    }

    @Test
    void shouldHandleUnicodeMessage() {
        // Given
        String unicodeMessage = "Hello, 世界! 🌍";

        // When
        String encrypted = encryptor.encrypt(unicodeMessage);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(unicodeMessage, decrypted);
    }

    @Test
    void shouldHandleLargeMessage() {
        // Given
        String largeMessage = "A".repeat(10000);

        // When
        String encrypted = encryptor.encrypt(largeMessage);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(largeMessage, decrypted);
    }

    @Test
    void shouldHandleJsonMessage() {
        // Given
        String jsonMessage = "{\"event\": \"user_login\", \"user_id\": 12345, \"timestamp\": \"2025-01-01T00:00:00Z\"}";

        // When
        String encrypted = encryptor.encrypt(jsonMessage);
        String decrypted = encryptor.decrypt(encrypted);

        // Then
        assertEquals(jsonMessage, decrypted);
    }

    @Test
    void shouldBeConsistentAcrossInstances() {
        // Given
        String message = "Cross-instance test";
        Encryptor encryptor2 = new Encryptor(TestConstants.TEST_SECRET);

        // When
        String encrypted = encryptor.encrypt(message);
        String decrypted = encryptor2.decrypt(encrypted);

        // Then
        assertEquals(message, decrypted);
    }

    @Test
    void shouldBeConsistentAcrossMultipleCycles() {
        // Given
        String message = "Consistency test message";

        // When & Then
        for (int i = 0; i < 100; i++) {
            String encrypted = encryptor.encrypt(message);
            String decrypted = encryptor.decrypt(encrypted);
            assertEquals(message, decrypted);
        }
    }

    @Test
    void shouldThrowExceptionForNullMessage() {
        // When & Then
        assertThrows(EncryptionException.class, () -> {
            encryptor.encrypt(null);
        });
    }

    @Test
    void shouldThrowExceptionForNullSecret() {
        // When & Then
        assertThrows(EncryptionException.class, () -> {
            new Encryptor(null);
        });
    }

    @Test
    void shouldThrowExceptionForEmptySecret() {
        // When & Then
        assertThrows(EncryptionException.class, () -> {
            new Encryptor("");
        });
    }

    @Test
    void shouldThrowExceptionForInvalidEncryptedData() {
        // Given
        String invalidEncrypted = "invalid-base64-data";

        // When & Then
        assertThrows(EncryptionException.class, () -> {
            encryptor.decrypt(invalidEncrypted);
        });
    }

    @Test
    void shouldThrowExceptionForCorruptedData() {
        // Given
        String originalMessage = "Hello, LogFlux!";
        String encrypted = encryptor.encrypt(originalMessage);
        String corrupted = encrypted.substring(0, encrypted.length() - 5) + "XXXXX";

        // When & Then
        assertThrows(EncryptionException.class, () -> {
            encryptor.decrypt(corrupted);
        });
    }

    @Test
    void shouldThrowExceptionForWrongSecret() {
        // Given
        String originalMessage = "Hello, LogFlux!";
        String encrypted = encryptor.encrypt(originalMessage);
        Encryptor wrongEncryptor = new Encryptor("wrong-secret");

        // When & Then
        assertThrows(EncryptionException.class, () -> {
            wrongEncryptor.decrypt(encrypted);
        });
    }

    @Test
    void shouldThrowExceptionForNullEncryptedMessage() {
        // When & Then
        assertThrows(EncryptionException.class, () -> {
            encryptor.decrypt(null);
        });
    }

    @Test
    void shouldThrowExceptionForEmptyEncryptedMessage() {
        // When & Then
        assertThrows(EncryptionException.class, () -> {
            encryptor.decrypt("");
        });
    }

    @Test
    void shouldGenerateRandomKey() {
        // When
        javax.crypto.SecretKey key = Encryptor.generateRandomKey();

        // Then
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertNotNull(key.getEncoded());
    }

    @Test
    void shouldProduceDifferentIVsForSameMessage() {
        // Given
        String message = "Same message";
        
        // When
        String encrypted1 = encryptor.encrypt(message);
        String encrypted2 = encryptor.encrypt(message);
        
        // Then
        assertNotEquals(encrypted1, encrypted2);
        
        // Decrypt both and verify they produce the same original message
        assertEquals(message, encryptor.decrypt(encrypted1));
        assertEquals(message, encryptor.decrypt(encrypted2));
    }

    @Test
    void shouldNotContainOriginalMessageInEncrypted() {
        // Given
        String message = "Predictable message";
        
        // When
        String encrypted = encryptor.encrypt(message);
        
        // Then
        assertFalse(encrypted.contains(message));
        assertFalse(encrypted.contains("Predictable"));
        assertFalse(encrypted.contains("message"));
    }
}