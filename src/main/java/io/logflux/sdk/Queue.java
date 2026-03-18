package io.logflux.sdk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Thread-safe bounded FIFO queue for log entries.
 */
public final class Queue {

    /**
     * Represents a queued entry.
     */
    static final class Entry {
        final String jsonPayload;
        final int level;
        final int entryType;

        Entry(String jsonPayload, int level, int entryType) {
            this.jsonPayload = jsonPayload;
            this.level = level;
            this.entryType = entryType;
        }
    }

    private final LinkedList<Entry> items;
    private final int maxSize;
    private volatile boolean closed;

    public Queue(int maxSize) {
        this.maxSize = maxSize > 0 ? maxSize : 1000;
        this.items = new LinkedList<>();
        this.closed = false;
    }

    /**
     * Adds an entry to the queue. Returns false if full or closed.
     */
    public synchronized boolean enqueue(Entry entry) {
        if (closed || items.size() >= maxSize) {
            return false;
        }
        items.addLast(entry);
        notifyAll();
        return true;
    }

    /**
     * Removes and returns the oldest entry, or null if empty.
     */
    public synchronized Entry dequeue() {
        if (items.isEmpty()) return null;
        return items.removeFirst();
    }

    /**
     * Removes up to n entries and returns them.
     */
    public synchronized List<Entry> dequeueBatch(int n) {
        if (items.isEmpty()) return null;
        int count = Math.min(n, items.size());
        List<Entry> batch = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            batch.add(items.removeFirst());
        }
        return batch;
    }

    /**
     * Blocks until an entry is available, the queue is closed, or the thread is interrupted.
     * Returns null on close or interrupt.
     */
    public synchronized Entry dequeueBlocking() {
        while (items.isEmpty()) {
            if (closed) return null;
            try {
                wait(100); // wake periodically to check closed flag
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return items.removeFirst();
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized boolean isEmpty() {
        return items.isEmpty();
    }

    public synchronized boolean isFull() {
        return items.size() >= maxSize;
    }

    public int capacity() {
        return maxSize;
    }

    public synchronized void clear() {
        items.clear();
    }

    public void close() {
        synchronized (this) {
            closed = true;
            notifyAll();
        }
    }

    public boolean isClosed() {
        return closed;
    }
}
