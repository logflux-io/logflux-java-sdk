package io.logflux.sdk;

import io.logflux.sdk.crypto.Crypto;
import io.logflux.sdk.transport.Discovery;
import io.logflux.sdk.transport.Handshake;
import io.logflux.sdk.transport.MultipartBuilder;
import io.logflux.sdk.transport.Sender;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Core client orchestrator. Manages queue, background workers, encryption,
 * rate limit handling, and stats tracking.
 *
 * Thread-safe. All public methods can be called from any thread.
 */
final class Client {

    // Drop reasons
    static final String DROP_QUEUE_OVERFLOW = "queue_overflow";
    static final String DROP_NETWORK_ERROR = "network_error";
    static final String DROP_SEND_ERROR = "send_error";
    static final String DROP_RATE_LIMITED = "ratelimit_backoff";
    static final String DROP_QUOTA_EXCEEDED = "quota_exceeded";
    static final String DROP_BEFORE_SEND = "before_send";

    private final Options options;
    private final Crypto encryptor;
    private final HttpClient httpClient;
    private final Queue queue;
    private final Discovery.EndpointInfo endpoints;
    private final String keyUuid;
    private final ExecutorService workerPool;

    // Sampler
    private final double sampleRate;
    private final java.util.Random samplerRng;
    private final Object samplerLock = new Object();

    // Stats (atomic for fast path)
    private final AtomicLong totalSent = new AtomicLong();
    private final AtomicLong totalQueued = new AtomicLong();
    private final AtomicLong totalDropped = new AtomicLong();
    private final ConcurrentHashMap<String, AtomicLong> dropReasons = new ConcurrentHashMap<>();
    private volatile String lastSendError;
    private volatile long lastSendTimeMs;

    // Rate limit state
    private volatile long rateLimitPauseUntilMs;
    private volatile int rateLimitLimit;
    private volatile int rateLimitRemaining;
    private volatile long rateLimitReset;

    // Quota state
    private final ConcurrentHashMap<String, Boolean> quotaBlocked = new ConcurrentHashMap<>();

    // Hooks
    private final Function<Map<String, Object>, Map<String, Object>> beforeSend;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendLog;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendError;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendMetric;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendEvent;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendAudit;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendTrace;
    private final Function<Map<String, Object>, Map<String, Object>> beforeSendTelemetry;

    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final boolean handshakeOK;

    /**
     * Creates and initializes the client: discovery, handshake, workers.
     */
    Client(Options options) throws Exception {
        this.options = options;
        this.beforeSend = options.beforeSend;
        this.beforeSendLog = options.beforeSendLog;
        this.beforeSendError = options.beforeSendError;
        this.beforeSendMetric = options.beforeSendMetric;
        this.beforeSendEvent = options.beforeSendEvent;
        this.beforeSendAudit = options.beforeSendAudit;
        this.beforeSendTrace = options.beforeSendTrace;
        this.beforeSendTelemetry = options.beforeSendTelemetry;

        // Validate API key
        validateApiKey(options.apiKey);

        // Sample rate
        double sr = options.sampleRate;
        if (sr <= 0) sr = 1.0;
        if (sr > 1.0) sr = 1.0;
        this.sampleRate = sr;
        this.samplerRng = new java.util.Random();

        // HTTP client
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(options.httpTimeoutMs))
                .build();

        Duration httpTimeout = Duration.ofMillis(options.httpTimeoutMs);

        // Endpoint discovery
        if (options.customEndpointUrl != null && !options.customEndpointUrl.isEmpty()) {
            this.endpoints = Discovery.customEndpoint(options.customEndpointUrl);
        } else {
            this.endpoints = Discovery.discover(options.apiKey, httpClient,
                    Duration.ofSeconds(10));
        }

        // Handshake
        Handshake.Result hsResult = Handshake.perform(
                endpoints.getHandshakeUrl(), options.apiKey, httpClient, httpTimeout);

        this.encryptor = new Crypto(hsResult.aesKey);
        // Zero source key material
        Arrays.fill(hsResult.aesKey, (byte) 0);
        this.keyUuid = hsResult.keyUuid;
        this.handshakeOK = true;

        // Queue
        this.queue = new Queue(options.queueSize > 0 ? options.queueSize : 1000);

        // Start background workers (daemon threads)
        int workers = options.workerCount > 0 ? options.workerCount : 2;
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "logflux-worker");
            t.setDaemon(true);
            return t;
        };
        this.workerPool = Executors.newFixedThreadPool(workers, tf);
        for (int i = 0; i < workers; i++) {
            workerPool.submit(this::workerLoop);
        }
    }

    /**
     * Enqueues a serialized payload for async sending.
     */
    void enqueue(String jsonPayload, int level, int entryType) {
        if (closed.get()) return;

        // Check quota
        String category = EntryType.category(entryType);
        if (quotaBlocked.containsKey(category)) {
            recordDrop(DROP_QUOTA_EXCEEDED, 1);
            return;
        }

        Queue.Entry entry = new Queue.Entry(jsonPayload, level, entryType);
        if (queue.enqueue(entry)) {
            totalQueued.incrementAndGet();
        } else {
            recordDrop(DROP_QUEUE_OVERFLOW, 1);
        }
    }

    /**
     * Checks if this entry should be sampled (sent).
     * Audit entries are always sent (never sampled).
     */
    boolean shouldSample(int entryType) {
        if (entryType == EntryType.AUDIT) return true; // compliance requirement
        if (sampleRate >= 1.0) return true;
        if (sampleRate <= 0.0) return false;
        synchronized (samplerLock) {
            return samplerRng.nextDouble() < sampleRate;
        }
    }

    /**
     * Applies a per-type hook. Returns null to drop the entry.
     */
    Map<String, Object> applyHook(Map<String, Object> payload, String typeName) {
        Function<Map<String, Object>, Map<String, Object>> hook = null;
        switch (typeName) {
            case "log": hook = beforeSendLog; break;
            case "error": hook = beforeSendError; break;
            case "metric": hook = beforeSendMetric; break;
            case "event": hook = beforeSendEvent; break;
            case "audit": hook = beforeSendAudit; break;
            case "trace": hook = beforeSendTrace; break;
            case "telemetry": hook = beforeSendTelemetry; break;
        }
        if (hook != null) {
            payload = hook.apply(payload);
            if (payload == null) return null;
        }
        if (beforeSend != null) {
            payload = beforeSend.apply(payload);
        }
        return payload;
    }

    /**
     * Returns current stats.
     */
    ClientStats getStats() {
        Map<String, Long> reasons = new HashMap<>();
        for (Map.Entry<String, AtomicLong> e : dropReasons.entrySet()) {
            reasons.put(e.getKey(), e.getValue().get());
        }
        return new ClientStats(
                totalSent.get(), totalDropped.get(), totalQueued.get(),
                queue.size(), queue.capacity(), reasons,
                lastSendError, lastSendTimeMs, handshakeOK
        );
    }

    boolean isActive() {
        return !closed.get() && handshakeOK;
    }

    /**
     * Flushes the queue within the given timeout.
     */
    void flush(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Closes the client: flushes, stops workers, zeros key material.
     */
    void close() {
        if (closed.getAndSet(true)) return;

        // Flush with 10s timeout
        flush(10000);

        queue.close();
        workerPool.shutdown();
        try {
            workerPool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        workerPool.shutdownNow();

        // Zero key material
        encryptor.close();
    }

    // --- Background worker ---

    private void workerLoop() {
        while (!closed.get() || !queue.isEmpty()) {
            Queue.Entry entry = queue.dequeueBlocking();
            if (entry == null) {
                if (closed.get()) return;
                continue;
            }

            // Rate limit pre-flight
            long pauseUntil = rateLimitPauseUntilMs;
            if (pauseUntil > 0 && System.currentTimeMillis() < pauseUntil) {
                // Re-enqueue if possible
                if (!queue.enqueue(entry)) {
                    recordDrop(DROP_RATE_LIMITED, 1);
                }
                try {
                    long sleepMs = pauseUntil - System.currentTimeMillis();
                    if (sleepMs > 0) Thread.sleep(Math.min(sleepMs, 5000));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }

            // Build batch
            List<MultipartBuilder.Part> parts = new ArrayList<>();
            parts.add(new MultipartBuilder.Part(entry.jsonPayload, entry.entryType, entry.level));

            int batchSize = options.batchSize > 0 ? options.batchSize : 100;
            if (batchSize > 1) {
                List<Queue.Entry> extra = queue.dequeueBatch(batchSize - 1);
                if (extra != null) {
                    for (Queue.Entry e : extra) {
                        parts.add(new MultipartBuilder.Part(e.jsonPayload, e.entryType, e.level));
                    }
                }
            }

            sendWithRetry(parts);
        }
    }

    private void sendWithRetry(List<MultipartBuilder.Part> parts) {
        int maxRetries = options.maxRetries > 0 ? options.maxRetries : 3;
        int attempt = 0;

        while (attempt <= maxRetries) {
            try {
                MultipartBuilder.Result body = MultipartBuilder.build(
                        parts, encryptor, keyUuid, options.enableCompression);

                Sender.SendResult result = Sender.send(
                        endpoints.getIngestUrl(), options.apiKey,
                        body.body, body.contentType, httpClient,
                        Duration.ofMillis(options.httpTimeoutMs));

                // Update rate limit info
                rateLimitLimit = result.rateLimitLimit;
                rateLimitRemaining = result.rateLimitRemaining;
                rateLimitReset = result.rateLimitReset;

                if (result.isSuccess()) {
                    totalSent.addAndGet(parts.size());
                    lastSendTimeMs = System.currentTimeMillis();
                    return;
                }

                // Rate limited: wait and don't count as retry attempt
                if (result.isRateLimited()) {
                    int retryAfter = result.retryAfterSeconds > 0
                            ? result.retryAfterSeconds : 60;
                    rateLimitPauseUntilMs = System.currentTimeMillis() +
                            (retryAfter * 1000L);
                    try {
                        Thread.sleep(Math.min(retryAfter * 1000L, 60000));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        recordDrop(DROP_RATE_LIMITED, parts.size());
                        return;
                    }
                    continue; // don't increment attempt
                }

                // Quota exceeded: block category
                if (result.isQuotaExceeded() && !parts.isEmpty()) {
                    String cat = EntryType.category(parts.get(0).entryType);
                    quotaBlocked.put(cat, Boolean.TRUE);
                    recordDrop(DROP_QUOTA_EXCEEDED, parts.size());
                    lastSendError = result.errorMessage;
                    return;
                }

                // Non-retryable error
                if (!result.isRetryable()) {
                    recordDrop(DROP_SEND_ERROR, parts.size());
                    lastSendError = result.errorMessage;
                    return;
                }

                // Retryable error
                lastSendError = result.errorMessage;
                attempt++;
                if (attempt <= maxRetries) {
                    long delay = calculateBackoffDelay(attempt);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        recordDrop(DROP_SEND_ERROR, parts.size());
                        return;
                    }
                }

            } catch (Exception e) {
                lastSendError = e.getMessage();
                attempt++;
                if (attempt <= maxRetries) {
                    long delay = calculateBackoffDelay(attempt);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        recordDrop(DROP_NETWORK_ERROR, parts.size());
                        return;
                    }
                }
            }
        }

        // All retries exhausted
        recordDrop(DROP_NETWORK_ERROR, parts.size());
    }

    private long calculateBackoffDelay(int attempt) {
        double delay = 1000.0 * Math.pow(2.0, attempt - 1); // 1s, 2s, 4s, ...
        if (delay > 30000) delay = 30000; // cap at 30s
        // 25% jitter
        double jitter = samplerRng.nextDouble() * 0.25;
        return (long) (delay * (1 + jitter));
    }

    private void recordDrop(String reason, long count) {
        totalDropped.addAndGet(count);
        dropReasons.computeIfAbsent(reason, k -> new AtomicLong()).addAndGet(count);
    }

    // --- API key validation ---

    static void validateApiKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("API key is required");
        }
        String[] parts = key.split("-", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid API key format: must be <region>-lf_<key>");
        }
        String region = parts[0];
        if (!("eu".equals(region) || "us".equals(region) || "ca".equals(region) ||
              "au".equals(region) || "ap".equals(region))) {
            throw new IllegalArgumentException(
                    "Invalid API key region: \"" + region + "\" (expected eu, us, ca, au, or ap)");
        }
        if (!parts[1].startsWith("lf_")) {
            throw new IllegalArgumentException("Invalid API key format: key must start with lf_");
        }
        if (parts[1].length() <= 3) {
            throw new IllegalArgumentException("Invalid API key format: key body is empty");
        }
    }
}
