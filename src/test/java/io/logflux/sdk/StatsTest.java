package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {

    @Test
    void emptyStats() {
        ClientStats stats = ClientStats.empty();
        assertEquals(0, stats.getEntriesSent());
        assertEquals(0, stats.getEntriesDropped());
        assertEquals(0, stats.getEntriesQueued());
        assertEquals(0, stats.getQueueSize());
        assertEquals(0, stats.getQueueCapacity());
        assertTrue(stats.getDropReasons().isEmpty());
        assertNull(stats.getLastSendError());
        assertEquals(0, stats.getLastSendTimeMs());
        assertFalse(stats.isHandshakeOK());
    }

    @Test
    void statsWithValues() {
        Map<String, Long> reasons = new HashMap<>();
        reasons.put("queue_overflow", 5L);
        reasons.put("network_error", 2L);

        ClientStats stats = new ClientStats(100, 7, 50, 10, 1000, reasons,
                "connection timeout", System.currentTimeMillis(), true);

        assertEquals(100, stats.getEntriesSent());
        assertEquals(7, stats.getEntriesDropped());
        assertEquals(50, stats.getEntriesQueued());
        assertEquals(10, stats.getQueueSize());
        assertEquals(1000, stats.getQueueCapacity());
        assertEquals(2, stats.getDropReasons().size());
        assertEquals(5L, stats.getDropReasons().get("queue_overflow"));
        assertEquals("connection timeout", stats.getLastSendError());
        assertTrue(stats.isHandshakeOK());
    }

    @Test
    void dropReasonsAreUnmodifiable() {
        Map<String, Long> reasons = new HashMap<>();
        reasons.put("test", 1L);
        ClientStats stats = new ClientStats(0, 0, 0, 0, 0, reasons, null, 0, false);
        assertThrows(UnsupportedOperationException.class, () ->
                stats.getDropReasons().put("new", 2L));
    }

    @Test
    void toStringContainsKeyFields() {
        ClientStats stats = new ClientStats(42, 3, 10, 5, 100, null, null, 0, true);
        String str = stats.toString();
        assertTrue(str.contains("sent=42"));
        assertTrue(str.contains("dropped=3"));
        assertTrue(str.contains("handshakeOK=true"));
    }

    @Test
    void statsWithoutClient() {
        // When SDK is not initialized, stats() should return empty stats
        ClientStats stats = LogFlux.stats();
        assertEquals(0, stats.getEntriesSent());
    }
}
