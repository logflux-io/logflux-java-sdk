package io.logflux.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogFluxExceptionTest {

    @Test
    void testConstructorWithMessage() {
        String message = "Test error message";
        LogFluxException exception = new LogFluxException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        String message = "Test error message";
        Throwable cause = new RuntimeException("Underlying cause");
        LogFluxException exception = new LogFluxException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals("Underlying cause", exception.getCause().getMessage());
    }

    @Test
    void testConstructorWithNullMessage() {
        LogFluxException exception = new LogFluxException(null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullCause() {
        String message = "Test error message";
        LogFluxException exception = new LogFluxException(message, null);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testConstructorWithNullMessageAndCause() {
        LogFluxException exception = new LogFluxException(null, null);
        
        assertNull(exception.getMessage());
        assertNull(exception.getCause());
    }

    @Test
    void testIsRuntimeException() {
        LogFluxException exception = new LogFluxException("Test");
        
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }

    @Test
    void testThrowAndCatch() {
        String message = "Operation failed";
        
        assertThrows(LogFluxException.class, () -> {
            throw new LogFluxException(message);
        });
        
        try {
            throw new LogFluxException(message);
        } catch (LogFluxException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    void testThrowAndCatchWithCause() {
        String message = "Operation failed";
        Throwable cause = new IllegalStateException("Invalid state");
        
        assertThrows(LogFluxException.class, () -> {
            throw new LogFluxException(message, cause);
        });
        
        try {
            throw new LogFluxException(message, cause);
        } catch (LogFluxException e) {
            assertEquals(message, e.getMessage());
            assertEquals(cause, e.getCause());
        }
    }

    @Test
    void testNestedExceptions() {
        Throwable rootCause = new NullPointerException("Null value");
        Throwable intermediateCause = new IllegalArgumentException("Invalid argument", rootCause);
        LogFluxException exception = new LogFluxException("LogFlux error", intermediateCause);
        
        assertEquals("LogFlux error", exception.getMessage());
        assertEquals(intermediateCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testStackTrace() {
        LogFluxException exception = new LogFluxException("Test error");
        
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
        assertEquals("testStackTrace", exception.getStackTrace()[0].getMethodName());
    }
}