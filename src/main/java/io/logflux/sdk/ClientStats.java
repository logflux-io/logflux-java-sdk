package io.logflux.sdk;

import java.util.Collections;
import java.util.Map;

/**
 * Runtime statistics for the LogFlux client.
 */
public final class ClientStats {
    private final long entriesSent;
    private final long entriesDropped;
    private final long entriesQueued;
    private final long queueSize;
    private final long queueCapacity;
    private final Map<String, Long> dropReasons;
    private final String lastSendError;
    private final long lastSendTimeMs;
    private final boolean handshakeOK;

    public ClientStats(long entriesSent, long entriesDropped, long entriesQueued,
                       long queueSize, long queueCapacity, Map<String, Long> dropReasons,
                       String lastSendError, long lastSendTimeMs, boolean handshakeOK) {
        this.entriesSent = entriesSent;
        this.entriesDropped = entriesDropped;
        this.entriesQueued = entriesQueued;
        this.queueSize = queueSize;
        this.queueCapacity = queueCapacity;
        this.dropReasons = dropReasons != null ? Collections.unmodifiableMap(dropReasons) : Collections.emptyMap();
        this.lastSendError = lastSendError;
        this.lastSendTimeMs = lastSendTimeMs;
        this.handshakeOK = handshakeOK;
    }

    /** Empty stats (returned when SDK is not initialized). */
    public static ClientStats empty() {
        return new ClientStats(0, 0, 0, 0, 0, null, null, 0, false);
    }

    public long getEntriesSent() { return entriesSent; }
    public long getEntriesDropped() { return entriesDropped; }
    public long getEntriesQueued() { return entriesQueued; }
    public long getQueueSize() { return queueSize; }
    public long getQueueCapacity() { return queueCapacity; }
    public Map<String, Long> getDropReasons() { return dropReasons; }
    public String getLastSendError() { return lastSendError; }
    public long getLastSendTimeMs() { return lastSendTimeMs; }
    public boolean isHandshakeOK() { return handshakeOK; }

    @Override
    public String toString() {
        return "ClientStats{sent=" + entriesSent + ", dropped=" + entriesDropped +
               ", queued=" + entriesQueued + ", queueSize=" + queueSize +
               ", handshakeOK=" + handshakeOK + "}";
    }
}
