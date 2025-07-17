package io.logflux.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogLevelTest {

    @Test
    void testLogLevelValues() {
        assertEquals(0, LogLevel.DEBUG.getValue());
        assertEquals(1, LogLevel.INFO.getValue());
        assertEquals(2, LogLevel.WARN.getValue());
        assertEquals(3, LogLevel.ERROR.getValue());
        assertEquals(4, LogLevel.FATAL.getValue());
    }

    @Test
    void testLogLevelNames() {
        assertEquals("DEBUG", LogLevel.DEBUG.getName());
        assertEquals("INFO", LogLevel.INFO.getName());
        assertEquals("WARN", LogLevel.WARN.getName());
        assertEquals("ERROR", LogLevel.ERROR.getName());
        assertEquals("FATAL", LogLevel.FATAL.getName());
    }

    @Test
    void testFromValue() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromValue(0));
        assertEquals(LogLevel.INFO, LogLevel.fromValue(1));
        assertEquals(LogLevel.WARN, LogLevel.fromValue(2));
        assertEquals(LogLevel.ERROR, LogLevel.fromValue(3));
        assertEquals(LogLevel.FATAL, LogLevel.fromValue(4));
    }

    @Test
    void testFromValueInvalid() {
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromValue(-1));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromValue(5));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromValue(100));
    }

    @Test
    void testFromName() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromName("DEBUG"));
        assertEquals(LogLevel.INFO, LogLevel.fromName("INFO"));
        assertEquals(LogLevel.WARN, LogLevel.fromName("WARN"));
        assertEquals(LogLevel.ERROR, LogLevel.fromName("ERROR"));
        assertEquals(LogLevel.FATAL, LogLevel.fromName("FATAL"));
    }

    @Test
    void testFromNameCaseInsensitive() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromName("debug"));
        assertEquals(LogLevel.INFO, LogLevel.fromName("info"));
        assertEquals(LogLevel.WARN, LogLevel.fromName("Warn"));
        assertEquals(LogLevel.ERROR, LogLevel.fromName("ErRoR"));
        assertEquals(LogLevel.FATAL, LogLevel.fromName("FaTaL"));
    }

    @Test
    void testFromNameInvalid() {
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromName("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromName(""));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.fromName("TRACE"));
    }

    @Test
    void testToString() {
        assertEquals("DEBUG", LogLevel.DEBUG.toString());
        assertEquals("INFO", LogLevel.INFO.toString());
        assertEquals("WARN", LogLevel.WARN.toString());
        assertEquals("ERROR", LogLevel.ERROR.toString());
        assertEquals("FATAL", LogLevel.FATAL.toString());
    }

    @Test
    void testEnumValues() {
        LogLevel[] values = LogLevel.values();
        assertEquals(5, values.length);
        assertEquals(LogLevel.DEBUG, values[0]);
        assertEquals(LogLevel.INFO, values[1]);
        assertEquals(LogLevel.WARN, values[2]);
        assertEquals(LogLevel.ERROR, values[3]);
        assertEquals(LogLevel.FATAL, values[4]);
    }

    @Test
    void testValueOf() {
        assertEquals(LogLevel.DEBUG, LogLevel.valueOf("DEBUG"));
        assertEquals(LogLevel.INFO, LogLevel.valueOf("INFO"));
        assertEquals(LogLevel.WARN, LogLevel.valueOf("WARN"));
        assertEquals(LogLevel.ERROR, LogLevel.valueOf("ERROR"));
        assertEquals(LogLevel.FATAL, LogLevel.valueOf("FATAL"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> LogLevel.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> LogLevel.valueOf("debug"));
    }
}