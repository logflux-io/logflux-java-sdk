package io.logflux.agent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a log entry to be sent to LogFlux Agent
 */
public class LogEntry {
    private String id;
    private String message;
    private String source;
    private int entryType;
    private int level;
    private long timestamp;
    private Map<String, String> labels;

    // Entry Types
    public static final int TYPE_LOG = 1;
    public static final int TYPE_METRIC = 2;
    public static final int TYPE_TRACE = 3;
    public static final int TYPE_EVENT = 4;
    public static final int TYPE_AUDIT = 5;

    // Log Levels (syslog)
    public static final int LEVEL_EMERGENCY = 0;
    public static final int LEVEL_ALERT = 1;
    public static final int LEVEL_CRITICAL = 2;
    public static final int LEVEL_ERROR = 3;
    public static final int LEVEL_WARNING = 4;
    public static final int LEVEL_NOTICE = 5;
    public static final int LEVEL_INFO = 6;
    public static final int LEVEL_DEBUG = 7;

    // Payload Types
    public static final String PAYLOAD_TYPE_SYSTEMD_JOURNAL = "systemd_journal";
    public static final String PAYLOAD_TYPE_SYSLOG = "syslog";
    public static final String PAYLOAD_TYPE_METRICS = "metrics";
    public static final String PAYLOAD_TYPE_APPLICATION = "application";
    public static final String PAYLOAD_TYPE_CONTAINER = "container";
    public static final String PAYLOAD_TYPE_GENERIC = "generic";
    public static final String PAYLOAD_TYPE_GENERIC_JSON = "generic_json";

    public LogEntry(String message) {
        this.id = UUID.randomUUID().toString();
        this.message = message;
        this.source = "java-sdk";
        this.entryType = TYPE_LOG;
        this.level = LEVEL_INFO;
        this.timestamp = Instant.now().getEpochSecond();
        this.labels = new HashMap<>();
    }

    // Builder pattern methods
    public LogEntry withSource(String source) {
        this.source = source;
        return this;
    }

    public LogEntry withType(int entryType) {
        this.entryType = entryType;
        return this;
    }

    public LogEntry withLevel(int level) {
        this.level = level;
        return this;
    }

    public LogEntry withTimestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public LogEntry withLabel(String key, String value) {
        this.labels.put(key, value);
        return this;
    }

    public LogEntry withPayloadType(String payloadType) {
        return withLabel("payload_type", payloadType);
    }

    // Convenience constructors for different types
    public static LogEntry newGenericEntry(String message) {
        String payloadType = isValidJson(message) ? PAYLOAD_TYPE_GENERIC_JSON : PAYLOAD_TYPE_GENERIC;
        return new LogEntry(message).withPayloadType(payloadType);
    }

    public static LogEntry newSyslogEntry(String message) {
        return new LogEntry(message).withPayloadType(PAYLOAD_TYPE_SYSLOG);
    }

    public static LogEntry newSystemdJournalEntry(String message) {
        return new LogEntry(message).withPayloadType(PAYLOAD_TYPE_SYSTEMD_JOURNAL);
    }

    public static LogEntry newMetricEntry(String message) {
        return new LogEntry(message).withType(TYPE_METRIC).withPayloadType(PAYLOAD_TYPE_METRICS);
    }

    public static LogEntry newApplicationEntry(String message) {
        return new LogEntry(message).withPayloadType(PAYLOAD_TYPE_APPLICATION);
    }

    public static LogEntry newContainerEntry(String message) {
        return new LogEntry(message).withPayloadType(PAYLOAD_TYPE_CONTAINER);
    }

    // JSON validation helper
    private static boolean isValidJson(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        
        str = str.trim();
        return (str.startsWith("{") && str.endsWith("}")) || 
               (str.startsWith("[") && str.endsWith("]"));
    }

    // Getters
    public String getId() { return id; }
    public String getMessage() { return message; }
    public String getSource() { return source; }
    public int getEntryType() { return entryType; }
    public int getLevel() { return level; }
    public long getTimestamp() { return timestamp; }
    public Map<String, String> getLabels() { return new HashMap<>(labels); }
}