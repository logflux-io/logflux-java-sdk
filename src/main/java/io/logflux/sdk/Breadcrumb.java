package io.logflux.sdk;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Breadcrumb entry and thread-safe ring buffer.
 */
public final class Breadcrumb {

    private final String timestamp;
    private final String category;
    private final String message;
    private final String level;
    private final Map<String, String> data;

    public Breadcrumb(String category, String message, String level, Map<String, String> data) {
        this.timestamp = Instant.now().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        this.category = category;
        this.message = message;
        this.level = level;
        this.data = data;
    }

    public Breadcrumb(String category, String message, Map<String, String> data) {
        this(category, message, null, data);
    }

    /**
     * Converts this breadcrumb to a map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("timestamp", timestamp);
        if (category != null && !category.isEmpty()) map.put("category", category);
        map.put("message", message);
        if (level != null && !level.isEmpty()) map.put("level", level);
        if (data != null && !data.isEmpty()) map.put("data", new LinkedHashMap<>(data));
        return map;
    }

    /**
     * Thread-safe ring buffer for breadcrumbs.
     */
    public static final class Ring {
        private final Breadcrumb[] items;
        private final int maxSize;
        private int position;
        private boolean full;

        public Ring(int maxSize) {
            this.maxSize = maxSize > 0 ? maxSize : 100;
            this.items = new Breadcrumb[this.maxSize];
            this.position = 0;
            this.full = false;
        }

        /**
         * Adds a breadcrumb to the ring buffer.
         */
        public synchronized void add(Breadcrumb b) {
            items[position] = b;
            position = (position + 1) % maxSize;
            if (position == 0) {
                full = true;
            }
        }

        /**
         * Returns a chronological snapshot of all breadcrumbs.
         */
        public synchronized List<Map<String, Object>> snapshot() {
            int count = full ? maxSize : position;
            if (count == 0) return null;

            List<Map<String, Object>> result = new ArrayList<>(count);
            if (full) {
                // Oldest entries start at position (wrapped around)
                for (int i = 0; i < maxSize; i++) {
                    int idx = (position + i) % maxSize;
                    if (items[idx] != null) {
                        result.add(items[idx].toMap());
                    }
                }
            } else {
                for (int i = 0; i < position; i++) {
                    if (items[i] != null) {
                        result.add(items[i].toMap());
                    }
                }
            }
            return result;
        }

        /**
         * Removes all breadcrumbs.
         */
        public synchronized void clear() {
            for (int i = 0; i < maxSize; i++) {
                items[i] = null;
            }
            position = 0;
            full = false;
        }

        /**
         * Returns the current number of breadcrumbs.
         */
        public synchronized int size() {
            return full ? maxSize : position;
        }
    }
}
