package io.logflux.adapters;

import io.logflux.client.ResilientClient;
import io.logflux.models.LogLevel;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Java Util Logging (JUL) adapter for LogFlux.
 */
public class JulAdapter extends Handler {
    private final ResilientClient client;

    /**
     * Creates a new JUL adapter.
     *
     * @param client The LogFlux client
     */
    public JulAdapter(ResilientClient client) {
        this.client = client;
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null) {
            return;
        }

        LogLevel logLevel = convertLevel(record.getLevel());
        String message = formatMessage(record);
        
        switch (logLevel) {
            case DEBUG:
                client.debug(message);
                break;
            case INFO:
                client.info(message);
                break;
            case WARN:
                client.warn(message);
                break;
            case ERROR:
                client.error(message);
                break;
            case FATAL:
                client.fatal(message);
                break;
        }
    }

    @Override
    public void flush() {
        // LogFlux handles flushing internally
    }

    @Override
    public void close() throws SecurityException {
        // LogFlux client handles closing
    }

    /**
     * Converts JUL Level to LogFlux LogLevel.
     *
     * @param level The JUL level
     * @return The corresponding LogFlux LogLevel
     */
    private LogLevel convertLevel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return LogLevel.ERROR;
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            return LogLevel.WARN;
        } else if (level.intValue() >= Level.INFO.intValue()) {
            return LogLevel.INFO;
        } else if (level.intValue() >= Level.CONFIG.intValue()) {
            return LogLevel.DEBUG;
        } else {
            return LogLevel.DEBUG;
        }
    }

    /**
     * Formats a log record into a message string.
     *
     * @param record The log record
     * @return The formatted message
     */
    private String formatMessage(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getMessage());
        
        if (record.getThrown() != null) {
            sb.append(" - ").append(record.getThrown().getMessage());
        }
        
        return sb.toString();
    }

    /**
     * Creates a JUL logger that sends logs to LogFlux.
     *
     * @param client The LogFlux client
     * @param name   The logger name
     * @return A configured JUL logger
     */
    public static java.util.logging.Logger createLogger(ResilientClient client, String name) {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(name);
        logger.addHandler(new JulAdapter(client));
        logger.setUseParentHandlers(false); // Don't use parent handlers
        return logger;
    }
}