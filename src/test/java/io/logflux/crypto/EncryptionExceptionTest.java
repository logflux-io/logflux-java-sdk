package io.logflux.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Encryption failed";
        EncryptionException exception = new EncryptionException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Decryption failed";
        Throwable cause = new RuntimeException("Invalid key");
        EncryptionException exception = new EncryptionException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("Invalid key", exception.getCause().getMessage());
    }

    @Test
    void testConstructorWithNullMessage() {
        EncryptionException exception = new EncryptionException(null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullCause() {
        String message = "Encryption error";
        EncryptionException exception = new EncryptionException(message, null);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullMessageAndCause() {
        EncryptionException exception = new EncryptionException(null, null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testIsRuntimeException() {
        EncryptionException exception = new EncryptionException("Test");
        
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    void testThrowAndCatch() {
        String message = "Key derivation failed";
        
        assertThrows(EncryptionException.class, () -> {
            throw new EncryptionException(message);
        });
        
        try {
            throw new EncryptionException(message);
        } catch (EncryptionException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    void testThrowAndCatchWithCause() {
        String message = "Cipher initialization failed";
        Throwable cause = new IllegalStateException("Invalid cipher mode");
        
        assertThrows(EncryptionException.class, () -> {
            throw new EncryptionException(message, cause);
        });
        
        try {
            throw new EncryptionException(message, cause);
        } catch (EncryptionException e) {
            assertEquals(message, e.getMessage());
            assertEquals(cause, e.getCause());
        }
    }

    @Test
    void testNestedExceptions() {
        Throwable rootCause = new IllegalArgumentException("Invalid algorithm");
        Throwable intermediateCause = new RuntimeException("Crypto provider error", rootCause);
        EncryptionException exception = new EncryptionException("Encryption failed", intermediateCause);
        
        assertEquals("Encryption failed", exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testStackTrace() {
        EncryptionException exception = new EncryptionException("Crypto error");
        
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
        assertEquals("testStackTrace", exception.getStackTrace()[0].getMethodName());
    }

    @Test
    void testCommonEncryptionScenarios() {
        // Test common encryption error scenarios
        EncryptionException keyError = new EncryptionException("Invalid key length");
        assertTrue(keyError.getMessage().contains("key"));
        
        EncryptionException algorithmError = new EncryptionException("Algorithm not supported", 
            new RuntimeException("AES/GCM/NoPadding not available"));
        assertTrue(algorithmError.getMessage().contains("Algorithm"));
        assertNotNull(algorithmError.getCause());
        
        EncryptionException paddingError = new EncryptionException("Padding error during decryption");
        assertTrue(paddingError.getMessage().contains("Padding"));
    }
}