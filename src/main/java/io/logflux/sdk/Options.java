package io.logflux.sdk;

import java.util.Map;
import java.util.function.Function;

/**
 * Configuration for the LogFlux SDK. Use the Builder pattern.
 *
 * Example:
 *   Options opts = Options.builder("eu-lf_your_api_key")
 *       .source("my-service")
 *       .environment("production")
 *       .build();
 *   LogFlux.init(opts);
 */
public final class Options {

    final String apiKey;
    final String source;
    final String environment;
    final String release;
    final String node;
    final String customEndpointUrl;
    final String zone;

    final int queueSize;
    final long flushIntervalMs;
    final int batchSize;
    final int workerCount;
    final int maxRetries;
    final long httpTimeoutMs;
    final boolean failsafe;
    final boolean enableCompression;
    final double sampleRate;
    final int maxBreadcrumbs;
    final boolean debug;

    // Hooks
    final Function<Map<String, Object>, Map<String, Object>> beforeSend;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendLog;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendError;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendMetric;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendEvent;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendAudit;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendTrace;
    final Function<Map<String, Object>, Map<String, Object>> beforeSendTelemetry;

    private Options(Builder b) {
        this.apiKey = b.apiKey;
        this.source = b.source;
        this.environment = b.environment;
        this.release = b.release;
        this.node = b.node;
        this.customEndpointUrl = b.customEndpointUrl;
        this.zone = b.zone;
        this.queueSize = b.queueSize;
        this.flushIntervalMs = b.flushIntervalMs;
        this.batchSize = b.batchSize;
        this.workerCount = b.workerCount;
        this.maxRetries = b.maxRetries;
        this.httpTimeoutMs = b.httpTimeoutMs;
        this.failsafe = b.failsafe;
        this.enableCompression = b.enableCompression;
        this.sampleRate = b.sampleRate;
        this.maxBreadcrumbs = b.maxBreadcrumbs;
        this.debug = b.debug;
        this.beforeSend = b.beforeSend;
        this.beforeSendLog = b.beforeSendLog;
        this.beforeSendError = b.beforeSendError;
        this.beforeSendMetric = b.beforeSendMetric;
        this.beforeSendEvent = b.beforeSendEvent;
        this.beforeSendAudit = b.beforeSendAudit;
        this.beforeSendTrace = b.beforeSendTrace;
        this.beforeSendTelemetry = b.beforeSendTelemetry;
    }

    /**
     * Creates a new builder with the given API key.
     */
    public static Builder builder(String apiKey) {
        return new Builder(apiKey);
    }

    /**
     * Loads options from environment variables.
     * Required: LOGFLUX_API_KEY
     * Optional: LOGFLUX_ENVIRONMENT, LOGFLUX_NODE, LOGFLUX_SOURCE, LOGFLUX_RELEASE,
     *           LOGFLUX_QUEUE_SIZE, LOGFLUX_FLUSH_INTERVAL, LOGFLUX_BATCH_SIZE,
     *           LOGFLUX_WORKER_COUNT, LOGFLUX_MAX_RETRIES, LOGFLUX_HTTP_TIMEOUT,
     *           LOGFLUX_FAILSAFE_MODE, LOGFLUX_ENABLE_COMPRESSION, LOGFLUX_SAMPLE_RATE,
     *           LOGFLUX_MAX_BREADCRUMBS, LOGFLUX_DEBUG
     */
    public static Options fromEnvironment() {
        return fromEnvironment(null);
    }

    /**
     * Loads options from environment variables with a specific node name.
     */
    public static Options fromEnvironment(String node) {
        String apiKey = System.getenv("LOGFLUX_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("LOGFLUX_API_KEY environment variable is required");
        }

        Builder b = builder(apiKey);
        b.environment(envOrDefault("LOGFLUX_ENVIRONMENT", ""));
        b.source(envOrDefault("LOGFLUX_SOURCE", ""));
        b.release(envOrDefault("LOGFLUX_RELEASE", ""));

        if (node != null && !node.isEmpty()) {
            b.node(node);
        } else {
            b.node(envOrDefault("LOGFLUX_NODE", ""));
        }

        b.customEndpointUrl(envOrDefault("LOGFLUX_CUSTOM_ENDPOINT_URL", ""));
        b.zone(envOrDefault("LOGFLUX_ZONE", ""));

        String qs = System.getenv("LOGFLUX_QUEUE_SIZE");
        if (qs != null) { try { b.queueSize(Integer.parseInt(qs)); } catch (NumberFormatException ignored) {} }

        String fi = System.getenv("LOGFLUX_FLUSH_INTERVAL");
        if (fi != null) { try { b.flushInterval(Long.parseLong(fi) * 1000); } catch (NumberFormatException ignored) {} }

        String bs = System.getenv("LOGFLUX_BATCH_SIZE");
        if (bs != null) { try { b.batchSize(Integer.parseInt(bs)); } catch (NumberFormatException ignored) {} }

        String wc = System.getenv("LOGFLUX_WORKER_COUNT");
        if (wc != null) { try { b.workerCount(Integer.parseInt(wc)); } catch (NumberFormatException ignored) {} }

        String mr = System.getenv("LOGFLUX_MAX_RETRIES");
        if (mr != null) { try { b.maxRetries(Integer.parseInt(mr)); } catch (NumberFormatException ignored) {} }

        String ht = System.getenv("LOGFLUX_HTTP_TIMEOUT");
        if (ht != null) { try { b.httpTimeout(Long.parseLong(ht) * 1000); } catch (NumberFormatException ignored) {} }

        String fm = System.getenv("LOGFLUX_FAILSAFE_MODE");
        if (fm != null) { b.failsafe(Boolean.parseBoolean(fm)); }

        String ec = System.getenv("LOGFLUX_ENABLE_COMPRESSION");
        if (ec != null) { b.enableCompression(Boolean.parseBoolean(ec)); }

        String sr = System.getenv("LOGFLUX_SAMPLE_RATE");
        if (sr != null) { try { b.sampleRate(Double.parseDouble(sr)); } catch (NumberFormatException ignored) {} }

        String mb = System.getenv("LOGFLUX_MAX_BREADCRUMBS");
        if (mb != null) { try { b.maxBreadcrumbs(Integer.parseInt(mb)); } catch (NumberFormatException ignored) {} }

        String dbg = System.getenv("LOGFLUX_DEBUG");
        if (dbg != null) { b.debug(Boolean.parseBoolean(dbg)); }

        return b.build();
    }

    private static String envOrDefault(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v != null && !v.isEmpty()) ? v : defaultValue;
    }

    public static final class Builder {
        private final String apiKey;
        private String source = "";
        private String environment = "";
        private String release = "";
        private String node = "";
        private String customEndpointUrl = "";
        private String zone = "";
        private int queueSize = 1000;
        private long flushIntervalMs = 5000;
        private int batchSize = 100;
        private int workerCount = 2;
        private int maxRetries = 3;
        private long httpTimeoutMs = 30000;
        private boolean failsafe = true;
        private boolean enableCompression = true;
        private double sampleRate = 1.0;
        private int maxBreadcrumbs = 100;
        private boolean debug = false;

        private Function<Map<String, Object>, Map<String, Object>> beforeSend;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendLog;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendError;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendMetric;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendEvent;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendAudit;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendTrace;
        private Function<Map<String, Object>, Map<String, Object>> beforeSendTelemetry;

        Builder(String apiKey) {
            this.apiKey = apiKey;
        }

        public Builder source(String s) { this.source = s != null ? s : ""; return this; }
        public Builder environment(String s) { this.environment = s != null ? s : ""; return this; }
        public Builder release(String s) { this.release = s != null ? s : ""; return this; }
        public Builder node(String s) { this.node = s != null ? s : ""; return this; }
        public Builder customEndpointUrl(String s) { this.customEndpointUrl = s != null ? s : ""; return this; }
        public Builder zone(String s) { this.zone = s != null ? s : ""; return this; }
        public Builder queueSize(int n) { this.queueSize = n; return this; }
        public Builder flushInterval(long ms) { this.flushIntervalMs = ms; return this; }
        public Builder batchSize(int n) { this.batchSize = n; return this; }
        public Builder workerCount(int n) { this.workerCount = n; return this; }
        public Builder maxRetries(int n) { this.maxRetries = n; return this; }
        public Builder httpTimeout(long ms) { this.httpTimeoutMs = ms; return this; }
        public Builder failsafe(boolean b) { this.failsafe = b; return this; }
        public Builder enableCompression(boolean b) { this.enableCompression = b; return this; }
        public Builder sampleRate(double d) { this.sampleRate = d; return this; }
        public Builder maxBreadcrumbs(int n) { this.maxBreadcrumbs = n; return this; }
        public Builder debug(boolean b) { this.debug = b; return this; }

        public Builder beforeSend(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSend = hook; return this;
        }
        public Builder beforeSendLog(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendLog = hook; return this;
        }
        public Builder beforeSendError(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendError = hook; return this;
        }
        public Builder beforeSendMetric(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendMetric = hook; return this;
        }
        public Builder beforeSendEvent(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendEvent = hook; return this;
        }
        public Builder beforeSendAudit(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendAudit = hook; return this;
        }
        public Builder beforeSendTrace(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendTrace = hook; return this;
        }
        public Builder beforeSendTelemetry(Function<Map<String, Object>, Map<String, Object>> hook) {
            this.beforeSendTelemetry = hook; return this;
        }

        public Options build() {
            return new Options(this);
        }
    }
}
