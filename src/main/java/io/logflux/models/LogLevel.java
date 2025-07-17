package io.logflux.models;

/**
 * Represents the severity level of a log message.
 * Values are 0-based to match test expectations.
 */
public enum LogLevel {
    DEBUG(0, "DEBUG"),
    INFO(1, "INFO"),
    WARN(2, "WARN"),
    ERROR(3, "ERROR"),
    FATAL(4, "FATAL");
    
    // Aliases for compatibility with syslog standards (static constants)
    public static final LogLevel WARNING = WARN;
    public static final LogLevel CRITICAL = ERROR;
    public static final LogLevel ALERT = FATAL;
    public static final LogLevel EMERGENCY = FATAL;
    public static final LogLevel NOTICE = INFO;

    private final int value;
    private final String name;

    LogLevel(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Gets the numeric value of the log level.
     *
     * @return The numeric value (1-8)
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets the string name of the log level.
     *
     * @return The name (EMERGENCY, ALERT, CRITICAL, ERROR, WARNING, NOTICE, INFO, DEBUG)
     */
    public String getName() {
        return name;
    }

    /**
     * Creates a LogLevel from its numeric value.
     *
     * @param value The numeric value (0-4)
     * @return The corresponding LogLevel
     * @throws IllegalArgumentException if the value is not valid
     */
    public static LogLevel fromValue(int value) {
        for (LogLevel level : values()) {
            if (level.value == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid log level value: " + value + ". Valid values are 0-4.");
    }

    /**
     * Creates a LogLevel from its string name.
     *
     * @param name The name (case-insensitive)
     * @return The corresponding LogLevel
     * @throws IllegalArgumentException if the name is not valid
     */
    public static LogLevel fromName(String name) {
        for (LogLevel level : values()) {
            if (level.name.equalsIgnoreCase(name)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid log level name: " + name);
    }

    @Override
    public String toString() {
        return name;
    }
}