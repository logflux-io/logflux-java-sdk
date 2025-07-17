package io.logflux.queue;

import io.logflux.models.LogEntry;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe queue for storing log entries before transmission.
 */
public class LogQueue {
    private final BlockingQueue<LogEntry> queue;
    private final int capacity;
    private final AtomicLong totalDropped = new AtomicLong(0);
    private final boolean failsafeMode;

    /**
     * Creates a new LogQueue with the specified capacity.
     *
     * @param capacity     The maximum number of log entries the queue can hold
     * @param failsafeMode Whether to drop logs when queue is full instead of blocking
     */
    public LogQueue(int capacity, boolean failsafeMode) {
        this.capacity = capacity;
        this.failsafeMode = failsafeMode;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Adds a log entry to the queue.
     * In failsafe mode, drops the entry if the queue is full.
     * In non-failsafe mode, blocks until space is available.
     *
     * @param entry The log entry to add
     * @return true if the entry was added successfully, false if dropped
     * @throws InterruptedException if interrupted while waiting (non-failsafe mode only)
     */
    public boolean offer(LogEntry entry) throws InterruptedException {
        if (failsafeMode) {
            boolean added = queue.offer(entry);
            if (!added) {
                totalDropped.incrementAndGet();
            }
            return added;
        } else {
            queue.put(entry);
            return true;
        }
    }

    /**
     * Adds a log entry to the queue with a timeout.
     *
     * @param entry   The log entry to add
     * @param timeout The maximum time to wait
     * @param unit    The time unit of the timeout
     * @return true if the entry was added successfully, false if timed out or dropped
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean offer(LogEntry entry, long timeout, TimeUnit unit) throws InterruptedException {
        boolean added = queue.offer(entry, timeout, unit);
        if (!added && failsafeMode) {
            totalDropped.incrementAndGet();
        }
        return added;
    }

    /**
     * Retrieves and removes the head of the queue, waiting if necessary.
     *
     * @return The head of the queue
     * @throws InterruptedException if interrupted while waiting
     */
    public LogEntry take() throws InterruptedException {
        return queue.take();
    }

    /**
     * Retrieves and removes the head of the queue, waiting up to the specified timeout.
     *
     * @param timeout The maximum time to wait
     * @param unit    The time unit of the timeout
     * @return The head of the queue, or null if timed out
     * @throws InterruptedException if interrupted while waiting
     */
    public LogEntry poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    /**
     * Retrieves and removes the head of the queue, or returns null if empty.
     *
     * @return The head of the queue, or null if empty
     */
    public LogEntry poll() {
        return queue.poll();
    }

    /**
     * Gets the current size of the queue.
     *
     * @return The number of entries in the queue
     */
    public int size() {
        return queue.size();
    }

    /**
     * Gets the capacity of the queue.
     *
     * @return The maximum number of entries the queue can hold
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Checks if the queue is empty.
     *
     * @return true if the queue is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Checks if the queue is full.
     *
     * @return true if the queue is at capacity
     */
    public boolean isFull() {
        return queue.remainingCapacity() == 0;
    }

    /**
     * Gets the total number of entries dropped due to full queue.
     *
     * @return The total dropped count
     */
    public long getTotalDropped() {
        return totalDropped.get();
    }

    /**
     * Gets the remaining capacity of the queue.
     *
     * @return The number of additional entries that can be added
     */
    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    /**
     * Clears all entries from the queue.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Drains all available entries from the queue.
     *
     * @return An array of all entries that were in the queue
     */
    public LogEntry[] drainAll() {
        LogEntry[] entries = new LogEntry[queue.size()];
        queue.drainTo(java.util.Arrays.asList(entries));
        return entries;
    }
}