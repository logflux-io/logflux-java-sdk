package io.logflux.adapters;

import io.logflux.client.ResilientClient;
import io.logflux.models.LogLevel;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Filter;

import java.io.Serializable;

/**
 * Log4j2 appender for LogFlux.
 */
@Plugin(name = "LogFlux", category = "Core", elementType = "appender", printObject = true)
public class Log4jAdapter extends AbstractAppender {
    private final ResilientClient client;

    /**
     * Creates a new Log4j adapter.
     *
     * @param name   The appender name
     * @param filter The filter
     * @param layout The layout
     * @param client The LogFlux client
     */
    protected Log4jAdapter(String name, Filter filter, Layout<? extends Serializable> layout, ResilientClient client) {
        super(name, filter, layout);
        this.client = client;
    }

    @Override
    public void append(LogEvent event) {
        if (event == null) {
            return;
        }

        LogLevel logLevel = convertLevel(event.getLevel());
        String message = formatMessage(event);
        
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

    /**
     * Converts Log4j Level to LogFlux LogLevel.
     *
     * @param level The Log4j level
     * @return The corresponding LogFlux LogLevel
     */
    private LogLevel convertLevel(org.apache.logging.log4j.Level level) {
        if (level.intLevel() <= org.apache.logging.log4j.Level.FATAL.intLevel()) {
            return LogLevel.FATAL;
        } else if (level.intLevel() <= org.apache.logging.log4j.Level.ERROR.intLevel()) {
            return LogLevel.ERROR;
        } else if (level.intLevel() <= org.apache.logging.log4j.Level.WARN.intLevel()) {
            return LogLevel.WARN;
        } else if (level.intLevel() <= org.apache.logging.log4j.Level.INFO.intLevel()) {
            return LogLevel.INFO;
        } else {
            return LogLevel.DEBUG;
        }
    }

    /**
     * Formats a log event into a message string.
     *
     * @param event The log event
     * @return The formatted message
     */
    private String formatMessage(LogEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.getMessage().getFormattedMessage());
        
        if (event.getThrown() != null) {
            sb.append(" - ").append(event.getThrown().getMessage());
        }
        
        return sb.toString();
    }

    /**
     * Factory method for creating LogFlux appenders.
     *
     * @param name   The appender name
     * @param filter The filter
     * @param layout The layout
     * @param client The LogFlux client
     * @return A new LogFlux appender
     */
    @PluginFactory
    public static Log4jAdapter createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Filter") Filter filter,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            ResilientClient client) {
        
        if (name == null) {
            name = "LogFlux";
        }
        
        return new Log4jAdapter(name, filter, layout, client);
    }

    /**
     * Creates a Log4j appender that sends logs to LogFlux.
     *
     * @param client The LogFlux client
     * @param name   The appender name
     * @return A configured Log4j appender
     */
    public static Log4jAdapter createAppender(ResilientClient client, String name) {
        return new Log4jAdapter(name, null, null, client);
    }
}