package io.logflux.adapters;

import io.logflux.client.ResilientClient;
import io.logflux.models.LogLevel;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * SLF4J adapter for LogFlux that implements the SLF4J Logger interface.
 */
public class Slf4jAdapter implements Logger {
    private final ResilientClient client;
    private final String name;

    /**
     * Creates a new SLF4J adapter.
     *
     * @param client The LogFlux client
     * @param name   The logger name
     */
    public Slf4jAdapter(ResilientClient client, String name) {
        this.client = client;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return true;
    }

    @Override
    public void trace(String msg) {
        client.debug(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        client.debug(formatMessage(format, arg));
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        client.debug(formatMessage(format, arg1, arg2));
    }

    @Override
    public void trace(String format, Object... arguments) {
        client.debug(formatMessage(format, arguments));
    }

    @Override
    public void trace(String msg, Throwable t) {
        client.debug(msg + " - " + t.getMessage());
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return true;
    }

    @Override
    public void trace(Marker marker, String msg) {
        trace(msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        trace(format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        trace(format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        trace(format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public void debug(String msg) {
        client.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        client.debug(formatMessage(format, arg));
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        client.debug(formatMessage(format, arg1, arg2));
    }

    @Override
    public void debug(String format, Object... arguments) {
        client.debug(formatMessage(format, arguments));
    }

    @Override
    public void debug(String msg, Throwable t) {
        client.debug(msg + " - " + t.getMessage());
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return true;
    }

    @Override
    public void debug(Marker marker, String msg) {
        debug(msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        debug(format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        debug(format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... argArray) {
        debug(format, argArray);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public void info(String msg) {
        client.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        client.info(formatMessage(format, arg));
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        client.info(formatMessage(format, arg1, arg2));
    }

    @Override
    public void info(String format, Object... arguments) {
        client.info(formatMessage(format, arguments));
    }

    @Override
    public void info(String msg, Throwable t) {
        client.info(msg + " - " + t.getMessage());
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return true;
    }

    @Override
    public void info(Marker marker, String msg) {
        info(msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        info(format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        info(format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... argArray) {
        info(format, argArray);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        client.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        client.warn(formatMessage(format, arg));
    }

    @Override
    public void warn(String format, Object... arguments) {
        client.warn(formatMessage(format, arguments));
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        client.warn(formatMessage(format, arg1, arg2));
    }

    @Override
    public void warn(String msg, Throwable t) {
        client.warn(msg + " - " + t.getMessage());
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return true;
    }

    @Override
    public void warn(Marker marker, String msg) {
        warn(msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        warn(format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        warn(format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... argArray) {
        warn(format, argArray);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public void error(String msg) {
        client.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        client.error(formatMessage(format, arg));
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        client.error(formatMessage(format, arg1, arg2));
    }

    @Override
    public void error(String format, Object... arguments) {
        client.error(formatMessage(format, arguments));
    }

    @Override
    public void error(String msg, Throwable t) {
        client.error(msg + " - " + t.getMessage());
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return true;
    }

    @Override
    public void error(Marker marker, String msg) {
        error(msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        error(format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        error(format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... argArray) {
        error(format, argArray);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        error(msg, t);
    }

    /**
     * Formats a message with arguments using simple string replacement.
     *
     * @param format The format string with {} placeholders
     * @param args   The arguments
     * @return The formatted message
     */
    private String formatMessage(String format, Object... args) {
        if (format == null || args == null || args.length == 0) {
            return format;
        }

        StringBuilder result = new StringBuilder();
        int argIndex = 0;
        int start = 0;
        int placeholder;

        while ((placeholder = format.indexOf("{}", start)) != -1) {
            result.append(format, start, placeholder);
            if (argIndex < args.length) {
                result.append(args[argIndex++]);
            } else {
                result.append("{}");
            }
            start = placeholder + 2;
        }

        result.append(format.substring(start));
        return result.toString();
    }
}