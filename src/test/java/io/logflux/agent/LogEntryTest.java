package io.logflux.agent;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogEntryTest {

    @Test
    void testLogEntryCreation() {
        LogEntry entry = new LogEntry("Test message");
        
        assertNotNull(entry.getId());
        assertEquals("Test message", entry.getMessage());
        assertEquals("java-sdk", entry.getSource());
        assertEquals(LogEntry.LEVEL_INFO, entry.getLevel());
        assertEquals(LogEntry.TYPE_LOG, entry.getEntryType());
        assertTrue(entry.getTimestamp() > 0);
        assertTrue(entry.getLabels().isEmpty());
    }

    @Test
    void testBuilderPatternMethods() {
        LogEntry entry = new LogEntry("Test message")
            .withSource("test-app")
            .withLevel(LogEntry.LEVEL_ERROR)
            .withType(LogEntry.TYPE_METRIC)
            .withLabel("key1", "value1")
            .withLabel("key2", "value2");

        assertEquals("test-app", entry.getSource());
        assertEquals(LogEntry.LEVEL_ERROR, entry.getLevel());
        assertEquals(LogEntry.TYPE_METRIC, entry.getEntryType());
        assertEquals("value1", entry.getLabels().get("key1"));
        assertEquals("value2", entry.getLabels().get("key2"));
    }

    @Test
    void testGenericEntry() {
        String jsonMessage = "{\"event\": \"test\", \"value\": 123}";
        LogEntry entry = LogEntry.newGenericEntry(jsonMessage);
        
        assertEquals(jsonMessage, entry.getMessage());
        assertEquals(LogEntry.PAYLOAD_TYPE_GENERIC_JSON, entry.getLabels().get("payload_type"));
    }

    @Test
    void testGenericEntryNonJson() {
        String textMessage = "Simple text message";
        LogEntry entry = LogEntry.newGenericEntry(textMessage);
        
        assertEquals(textMessage, entry.getMessage());
        assertEquals(LogEntry.PAYLOAD_TYPE_GENERIC, entry.getLabels().get("payload_type"));
    }

    @Test
    void testMetricEntry() {
        String metricData = "{\"cpu\": 45.2, \"memory\": 1024}";
        LogEntry entry = LogEntry.newMetricEntry(metricData);
        
        assertEquals(metricData, entry.getMessage());
        assertEquals(LogEntry.TYPE_METRIC, entry.getEntryType());
        assertEquals(LogEntry.PAYLOAD_TYPE_METRICS, entry.getLabels().get("payload_type"));
    }

    @Test
    void testSyslogEntry() {
        LogEntry entry = LogEntry.newSyslogEntry("syslog message");
        assertEquals(LogEntry.PAYLOAD_TYPE_SYSLOG, entry.getLabels().get("payload_type"));
    }

    @Test
    void testApplicationEntry() {
        LogEntry entry = LogEntry.newApplicationEntry("app message");
        assertEquals(LogEntry.PAYLOAD_TYPE_APPLICATION, entry.getLabels().get("payload_type"));
    }

    @Test
    void testContainerEntry() {
        LogEntry entry = LogEntry.newContainerEntry("container message");
        assertEquals(LogEntry.PAYLOAD_TYPE_CONTAINER, entry.getLabels().get("payload_type"));
    }

    @Test
    void testSystemdJournalEntry() {
        LogEntry entry = LogEntry.newSystemdJournalEntry("journal message");
        assertEquals(LogEntry.PAYLOAD_TYPE_SYSTEMD_JOURNAL, entry.getLabels().get("payload_type"));
    }

    @Test
    void testWithPayloadType() {
        LogEntry entry = new LogEntry("test message")
            .withPayloadType("custom_type");
        assertEquals("custom_type", entry.getLabels().get("payload_type"));
    }

    @Test
    void testWithTimestamp() {
        long customTimestamp = 1234567890L;
        LogEntry entry = new LogEntry("test message")
            .withTimestamp(customTimestamp);
        assertEquals(customTimestamp, entry.getTimestamp());
    }
}