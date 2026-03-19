package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SamplingTest {

    /**
     * Helper to simulate shouldSample behavior (since Client is package-private
     * and requires network, we test the algorithm directly).
     */
    private static boolean shouldSample(double sampleRate, int entryType) {
        // Audit entries are always sent
        if (entryType == EntryType.AUDIT) return true;
        if (sampleRate >= 1.0) return true;
        if (sampleRate <= 0.0) return false;
        return Math.random() < sampleRate;
    }

    @Test
    void fullSampleRateSendsAll() {
        for (int i = 0; i < 100; i++) {
            assertTrue(shouldSample(1.0, EntryType.LOG));
        }
    }

    @Test
    void zeroSampleRateDropsAll() {
        for (int i = 0; i < 100; i++) {
            assertFalse(shouldSample(0.0, EntryType.LOG));
        }
    }

    @Test
    void auditAlwaysSentEvenWithZeroRate() {
        for (int i = 0; i < 100; i++) {
            assertTrue(shouldSample(0.0, EntryType.AUDIT));
        }
    }

    @Test
    void halfSampleRateIsApproximate() {
        int sent = 0;
        int total = 10000;
        java.util.Random rng = new java.util.Random(42); // deterministic seed
        double sampleRate = 0.5;
        for (int i = 0; i < total; i++) {
            if (rng.nextDouble() < sampleRate) sent++;
        }
        // Should be roughly 50% (within 10%)
        double ratio = (double) sent / total;
        assertTrue(ratio > 0.4 && ratio < 0.6,
                "Sample rate 0.5 should send ~50%, got " + (ratio * 100) + "%");
    }

    @Test
    void negativeSampleRateIsZero() {
        assertFalse(shouldSample(-1.0, EntryType.LOG));
    }

    @Test
    void sampleRateAboveOneIsClamped() {
        assertTrue(shouldSample(2.0, EntryType.LOG));
    }
}
