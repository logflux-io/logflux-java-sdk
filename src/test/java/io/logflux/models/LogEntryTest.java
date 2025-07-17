package io.logflux.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LogEntryTest {

    private ObjectMapper objectMapper;
    private Instant testTimestamp;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        testTimestamp = Instant.parse("2025-01-01T00:00:00Z");
    }

    @Test
    void testConstructorWithValidParameters() {
        LogEntry entry = new LogEntry("node1", "encrypted-payload", LogLevel.INFO, testTimestamp);
        
        assertEquals("node1", entry.getNode());
        assertEquals("encrypted-payload", entry.getPayload());
        assertEquals(1, entry.getLogLevel());
        assertEquals(testTimestamp, entry.getTimestamp());
    }

    @Test
    void testConstructorWithNullNode() {
        assertThrows(NullPointerException.class, () -> 
            new LogEntry(null, "payload", LogLevel.INFO, testTimestamp)
        );
    }

    @Test
    void testConstructorWithNullPayload() {
        assertThrows(NullPointerException.class, () -> 
            new LogEntry("node", null, LogLevel.INFO, testTimestamp)
        );
    }

    @Test
    void testConstructorWithNullLogLevel() {
        assertThrows(NullPointerException.class, () -> 
            new LogEntry("node", "payload", null, testTimestamp)
        );
    }

    @Test
    void testConstructorWithNullTimestamp() {
        assertThrows(NullPointerException.class, () -> 
            new LogEntry("node", "payload", LogLevel.INFO, null)
        );
    }

    @Test
    void testJsonConstructor() {
        LogEntry entry = new LogEntry("node1", "encrypted-payload", 2, testTimestamp);
        
        assertEquals("node1", entry.getNode());
        assertEquals("encrypted-payload", entry.getPayload());
        assertEquals(2, entry.getLogLevel());
        assertEquals(testTimestamp, entry.getTimestamp());
    }

    @Test
    void testEquals() {
        LogEntry entry1 = new LogEntry("node1", "payload", LogLevel.INFO, testTimestamp);
        LogEntry entry2 = new LogEntry("node1", "payload", LogLevel.INFO, testTimestamp);
        LogEntry entry3 = new LogEntry("node2", "payload", LogLevel.INFO, testTimestamp);
        LogEntry entry4 = new LogEntry("node1", "payload2", LogLevel.INFO, testTimestamp);
        LogEntry entry5 = new LogEntry("node1", "payload", LogLevel.ERROR, testTimestamp);
        LogEntry entry6 = new LogEntry("node1", "payload", LogLevel.INFO, testTimestamp.plusSeconds(1));

        assertEquals(entry1, entry2);
        assertNotEquals(entry1, entry3);
        assertNotEquals(entry1, entry4);
        assertNotEquals(entry1, entry5);
        assertNotEquals(entry1, entry6);
        assertNotEquals(entry1, null);
        assertNotEquals(entry1, new Object());
        assertEquals(entry1, entry1);
    }

    @Test
    void testHashCode() {
        LogEntry entry1 = new LogEntry("node1", "payload", LogLevel.INFO, testTimestamp);
        LogEntry entry2 = new LogEntry("node1", "payload", LogLevel.INFO, testTimestamp);
        LogEntry entry3 = new LogEntry("node2", "payload", LogLevel.INFO, testTimestamp);

        assertEquals(entry1.hashCode(), entry2.hashCode());
        assertNotEquals(entry1.hashCode(), entry3.hashCode());
    }

    @Test
    void testToString() {
        LogEntry entry = new LogEntry("node1", "payload", LogLevel.INFO, testTimestamp);
        String toString = entry.toString();

        assertTrue(toString.contains("node1"));
        assertTrue(toString.contains("payload"));
        assertTrue(toString.contains("1"));
        assertTrue(toString.contains(testTimestamp.toString()));
    }

    @Test
    void testJsonSerialization() throws Exception {
        LogEntry entry = new LogEntry("node1", "encrypted-payload", LogLevel.WARN, testTimestamp);
        
        String json = objectMapper.writeValueAsString(entry);
        
        assertTrue(json.contains("\"node\":\"node1\""));
        assertTrue(json.contains("\"payload\":\"encrypted-payload\""));
        assertTrue(json.contains("\"loglevel\":2"));
        assertTrue(json.contains("\"timestamp\":"));
    }

    @Test
    void testJsonDeserialization() throws Exception {
        String json = "{\"node\":\"node1\",\"payload\":\"encrypted-payload\",\"loglevel\":3,\"timestamp\":1735689600.0}";
        
        LogEntry entry = objectMapper.readValue(json, LogEntry.class);
        
        assertEquals("node1", entry.getNode());
        assertEquals("encrypted-payload", entry.getPayload());
        assertEquals(3, entry.getLogLevel());
        assertEquals(testTimestamp, entry.getTimestamp());
    }

    @Test
    void testAllLogLevels() {
        for (LogLevel level : LogLevel.values()) {
            LogEntry entry = new LogEntry("node", "payload", level, testTimestamp);
            assertEquals(level.getValue(), entry.getLogLevel());
        }
    }
}