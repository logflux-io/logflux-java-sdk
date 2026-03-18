package io.logflux.sdk;

/**
 * Entry type constants for LogFlux payload types.
 */
public final class EntryType {
    public static final int LOG               = 1;
    public static final int METRIC            = 2;
    public static final int TRACE             = 3;
    public static final int EVENT             = 4;
    public static final int AUDIT             = 5;
    public static final int TELEMETRY         = 6;
    public static final int TELEMETRY_MANAGED = 7;

    /** Default payload type: AES-256-GCM + gzip (types 1-6). */
    public static final int PAYLOAD_AES256GCM_GZIP_JSON = 1;
    /** Gzip-only payload type (type 7). */
    public static final int PAYLOAD_GZIP_JSON = 3;

    private EntryType() {}

    public static boolean isValid(int entryType) {
        return entryType >= 1 && entryType <= 7;
    }

    /**
     * Returns true if the entry type requires end-to-end encryption (types 1-6).
     */
    public static boolean requiresEncryption(int entryType) {
        return entryType >= 1 && entryType <= 6;
    }

    /**
     * Returns the default payload type for an entry type.
     */
    public static int defaultPayloadType(int entryType) {
        if (entryType == TELEMETRY_MANAGED) {
            return PAYLOAD_GZIP_JSON;
        }
        return PAYLOAD_AES256GCM_GZIP_JSON;
    }

    /**
     * Maps an entry type to its pricing category.
     */
    public static String category(int entryType) {
        switch (entryType) {
            case LOG:
            case METRIC:
            case EVENT:
                return "events";
            case TRACE:
            case TELEMETRY:
            case TELEMETRY_MANAGED:
                return "traces";
            case AUDIT:
                return "audit";
            default:
                return "events";
        }
    }

    /**
     * Returns the type name string for JSON payloads.
     */
    public static String typeName(int entryType) {
        switch (entryType) {
            case LOG:               return "log";
            case METRIC:            return "metric";
            case TRACE:             return "trace";
            case EVENT:             return "event";
            case AUDIT:             return "audit";
            case TELEMETRY:
            case TELEMETRY_MANAGED: return "telemetry";
            default:                return "log";
        }
    }
}
