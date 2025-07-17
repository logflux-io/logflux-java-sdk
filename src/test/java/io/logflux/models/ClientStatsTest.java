package io.logflux.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientStatsTest {

    @Test
    void testConstructorAndGetters() {
        ClientStats stats = new ClientStats(100L, 10L, 5L, 50, 100);
        
        assertEquals(100L, stats.getTotalSent());
        assertEquals(10L, stats.getTotalFailed());
        assertEquals(5L, stats.getTotalDropped());
        assertEquals(50, stats.getQueueSize());
        assertEquals(100, stats.getQueueCapacity());
    }

    @Test
    void testIsQueueFull() {
        ClientStats notFull = new ClientStats(0L, 0L, 0L, 99, 100);
        assertFalse(notFull.isQueueFull());
        
        ClientStats full = new ClientStats(0L, 0L, 0L, 100, 100);
        assertTrue(full.isQueueFull());
        
        ClientStats overFull = new ClientStats(0L, 0L, 0L, 101, 100);
        assertTrue(overFull.isQueueFull());
        
        ClientStats empty = new ClientStats(0L, 0L, 0L, 0, 100);
        assertFalse(empty.isQueueFull());
    }

    @Test
    void testGetQueueUtilization() {
        ClientStats empty = new ClientStats(0L, 0L, 0L, 0, 100);
        assertEquals(0.0, empty.getQueueUtilization(), 0.001);
        
        ClientStats half = new ClientStats(0L, 0L, 0L, 50, 100);
        assertEquals(0.5, half.getQueueUtilization(), 0.001);
        
        ClientStats full = new ClientStats(0L, 0L, 0L, 100, 100);
        assertEquals(1.0, full.getQueueUtilization(), 0.001);
        
        ClientStats quarter = new ClientStats(0L, 0L, 0L, 25, 100);
        assertEquals(0.25, quarter.getQueueUtilization(), 0.001);
        
        ClientStats zeroCapacity = new ClientStats(0L, 0L, 0L, 0, 0);
        assertEquals(0.0, zeroCapacity.getQueueUtilization(), 0.001);
    }

    @Test
    void testEquals() {
        ClientStats stats1 = new ClientStats(100L, 10L, 5L, 50, 100);
        ClientStats stats2 = new ClientStats(100L, 10L, 5L, 50, 100);
        ClientStats stats3 = new ClientStats(200L, 10L, 5L, 50, 100);
        ClientStats stats4 = new ClientStats(100L, 20L, 5L, 50, 100);
        ClientStats stats5 = new ClientStats(100L, 10L, 10L, 50, 100);
        ClientStats stats6 = new ClientStats(100L, 10L, 5L, 60, 100);
        ClientStats stats7 = new ClientStats(100L, 10L, 5L, 50, 200);

        assertEquals(stats1, stats2);
        assertNotEquals(stats1, stats3);
        assertNotEquals(stats1, stats4);
        assertNotEquals(stats1, stats5);
        assertNotEquals(stats1, stats6);
        assertNotEquals(stats1, stats7);
        assertNotEquals(stats1, null);
        assertNotEquals(stats1, new Object());
        assertEquals(stats1, stats1);
    }

    @Test
    void testHashCode() {
        ClientStats stats1 = new ClientStats(100L, 10L, 5L, 50, 100);
        ClientStats stats2 = new ClientStats(100L, 10L, 5L, 50, 100);
        ClientStats stats3 = new ClientStats(200L, 10L, 5L, 50, 100);

        assertEquals(stats1.hashCode(), stats2.hashCode());
        assertNotEquals(stats1.hashCode(), stats3.hashCode());
    }

    @Test
    void testToString() {
        ClientStats stats = new ClientStats(100L, 10L, 5L, 50, 100);
        String toString = stats.toString();

        assertTrue(toString.contains("totalSent=100"));
        assertTrue(toString.contains("totalFailed=10"));
        assertTrue(toString.contains("totalDropped=5"));
        assertTrue(toString.contains("queueSize=50"));
        assertTrue(toString.contains("queueCapacity=100"));
    }

    @Test
    void testWithZeroValues() {
        ClientStats stats = new ClientStats(0L, 0L, 0L, 0, 0);
        
        assertEquals(0L, stats.getTotalSent());
        assertEquals(0L, stats.getTotalFailed());
        assertEquals(0L, stats.getTotalDropped());
        assertEquals(0, stats.getQueueSize());
        assertEquals(0, stats.getQueueCapacity());
        assertTrue(stats.isQueueFull());
        assertEquals(0.0, stats.getQueueUtilization(), 0.001);
    }

    @Test
    void testWithLargeValues() {
        long largeValue = Long.MAX_VALUE;
        ClientStats stats = new ClientStats(largeValue, largeValue, largeValue, Integer.MAX_VALUE, Integer.MAX_VALUE);
        
        assertEquals(largeValue, stats.getTotalSent());
        assertEquals(largeValue, stats.getTotalFailed());
        assertEquals(largeValue, stats.getTotalDropped());
        assertEquals(Integer.MAX_VALUE, stats.getQueueSize());
        assertEquals(Integer.MAX_VALUE, stats.getQueueCapacity());
        assertTrue(stats.isQueueFull());
        assertEquals(1.0, stats.getQueueUtilization(), 0.001);
    }
}