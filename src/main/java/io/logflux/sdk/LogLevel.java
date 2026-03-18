package io.logflux.sdk;

/**
 * Log level constants following syslog severity (1-8).
 */
public final class LogLevel {
    public static final int EMERGENCY = 1;
    public static final int ALERT     = 2;
    public static final int CRITICAL  = 3;
    public static final int ERROR     = 4;
    public static final int WARNING   = 5;
    public static final int NOTICE    = 6;
    public static final int INFO      = 7;
    public static final int DEBUG     = 8;

    private LogLevel() {}

    public static boolean isValid(int level) {
        return level >= 1 && level <= 8;
    }

    public static String toString(int level) {
        switch (level) {
            case EMERGENCY: return "emergency";
            case ALERT:     return "alert";
            case CRITICAL:  return "critical";
            case ERROR:     return "error";
            case WARNING:   return "warning";
            case NOTICE:    return "notice";
            case INFO:      return "info";
            case DEBUG:     return "debug";
            default:        return "unknown";
        }
    }

    /**
     * Returns a short category string for breadcrumb classification.
     */
    static String toCategory(int level) {
        if (level <= CRITICAL) return "error";
        if (level == ERROR) return "error";
        if (level == WARNING) return "warning";
        return "info";
    }
}
