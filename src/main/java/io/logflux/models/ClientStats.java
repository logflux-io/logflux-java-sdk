package io.logflux.models;

import java.util.Objects;

/**
 * Statistics about the LogFlux client performance.
 */
public class ClientStats {
    private final long totalSent;
    private final long totalFailed;
    private final long totalDropped;
    private final int queueSize;
    private final int queueCapacity;

    /**
     * Creates new ClientStats.
     *
     * @param totalSent     Total number of logs sent successfully
     * @param totalFailed   Total number of logs that failed to send
     * @param totalDropped  Total number of logs dropped due to full queue
     * @param queueSize     Current number of logs in queue
     * @param queueCapacity Maximum queue capacity
     */
    public ClientStats(long totalSent, long totalFailed, long totalDropped, int queueSize, int queueCapacity) {
        this.totalSent = totalSent;
        this.totalFailed = totalFailed;
        this.totalDropped = totalDropped;
        this.queueSize = queueSize;
        this.queueCapacity = queueCapacity;
    }

    /**
     * Gets the total number of logs sent successfully.
     *
     * @return Total sent count
     */
    public long getTotalSent() {
        return totalSent;
    }

    /**
     * Gets the total number of logs that failed to send.
     *
     * @return Total failed count
     */
    public long getTotalFailed() {
        return totalFailed;
    }

    /**
     * Gets the total number of logs dropped due to full queue.
     *
     * @return Total dropped count
     */
    public long getTotalDropped() {
        return totalDropped;
    }

    /**
     * Gets the current number of logs in queue.
     *
     * @return Current queue size
     */
    public int getQueueSize() {
        return queueSize;
    }

    /**
     * Gets the maximum queue capacity.
     *
     * @return Queue capacity
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    /**
     * Checks if the queue is full.
     *
     * @return true if queue is at capacity
     */
    public boolean isQueueFull() {
        return queueSize >= queueCapacity;
    }

    /**
     * Gets the queue utilization percentage.
     *
     * @return Queue utilization (0.0 to 1.0)
     */
    public double getQueueUtilization() {
        return queueCapacity > 0 ? (double) queueSize / queueCapacity : 0.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClientStats that = (ClientStats) o;
        return totalSent == that.totalSent &&
                totalFailed == that.totalFailed &&
                totalDropped == that.totalDropped &&
                queueSize == that.queueSize &&
                queueCapacity == that.queueCapacity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalSent, totalFailed, totalDropped, queueSize, queueCapacity);
    }

    @Override
    public String toString() {
        return "ClientStats{" +
                "totalSent=" + totalSent +
                ", totalFailed=" + totalFailed +
                ", totalDropped=" + totalDropped +
                ", queueSize=" + queueSize +
                ", queueCapacity=" + queueCapacity +
                '}';
    }
}