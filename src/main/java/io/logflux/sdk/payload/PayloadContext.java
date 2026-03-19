package io.logflux.sdk.payload;

import java.util.HashMap;
import java.util.Map;

/**
 * Global context that gets auto-attached to all payloads.
 * Thread-safe via synchronized methods.
 */
public final class PayloadContext {
    private static String globalSource = "";
    private static String globalEnvironment = "";
    private static String globalRelease = "";

    private PayloadContext() {}

    /**
     * Configure the global payload context. Called by LogFlux.init().
     */
    public static synchronized void configure(String source, String environment, String release) {
        globalSource = source != null ? source : "";
        globalEnvironment = environment != null ? environment : "";
        globalRelease = release != null ? release : "";
    }

    /**
     * Returns the configured global source.
     */
    public static synchronized String getSource() {
        return globalSource;
    }

    /**
     * Applies global context to a payload map.
     * Sets source if empty and adds environment/release to meta.
     */
    public static synchronized void apply(Map<String, Object> payload) {
        if (payload == null) return;

        // Apply source if not already set
        Object existingSource = payload.get("source");
        if ((existingSource == null || "".equals(existingSource)) && !globalSource.isEmpty()) {
            payload.put("source", globalSource);
        }

        // Build meta map
        if (!globalEnvironment.isEmpty() || !globalRelease.isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> existingMeta = (Map<String, Object>) payload.get("meta");
            if (existingMeta == null) {
                Map<String, Object> meta = new HashMap<>();
                if (!globalEnvironment.isEmpty()) meta.put("environment", globalEnvironment);
                if (!globalRelease.isEmpty()) meta.put("release", globalRelease);
                payload.put("meta", meta);
            } else {
                // Don't overwrite user-set meta keys
                if (!globalEnvironment.isEmpty() && !existingMeta.containsKey("environment")) {
                    existingMeta.put("environment", globalEnvironment);
                }
                if (!globalRelease.isEmpty() && !existingMeta.containsKey("release")) {
                    existingMeta.put("release", globalRelease);
                }
            }
        }
    }

    /**
     * Reset context (used for testing).
     */
    public static synchronized void reset() {
        globalSource = "";
        globalEnvironment = "";
        globalRelease = "";
    }
}
