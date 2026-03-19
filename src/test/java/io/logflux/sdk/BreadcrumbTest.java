package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BreadcrumbTest {

    @Test
    void addAndSnapshot() {
        Breadcrumb.Ring ring = new Breadcrumb.Ring(10);
        ring.add(new Breadcrumb("http", "GET /api/users", null));
        ring.add(new Breadcrumb("log", "processing request", "info", null));

        List<Map<String, Object>> snapshot = ring.snapshot();
        assertNotNull(snapshot);
        assertEquals(2, snapshot.size());
        assertEquals("http", snapshot.get(0).get("category"));
        assertEquals("GET /api/users", snapshot.get(0).get("message"));
        assertEquals("log", snapshot.get(1).get("category"));
        assertEquals("info", snapshot.get(1).get("level"));
    }

    @Test
    void ringBufferWrapsAround() {
        Breadcrumb.Ring ring = new Breadcrumb.Ring(3);
        ring.add(new Breadcrumb("a", "msg1", null));
        ring.add(new Breadcrumb("b", "msg2", null));
        ring.add(new Breadcrumb("c", "msg3", null));
        ring.add(new Breadcrumb("d", "msg4", null)); // overwrites msg1

        List<Map<String, Object>> snapshot = ring.snapshot();
        assertNotNull(snapshot);
        assertEquals(3, snapshot.size());
        // Should be in chronological order: msg2, msg3, msg4
        assertEquals("msg2", snapshot.get(0).get("message"));
        assertEquals("msg3", snapshot.get(1).get("message"));
        assertEquals("msg4", snapshot.get(2).get("message"));
    }

    @Test
    void emptySnapshot() {
        Breadcrumb.Ring ring = new Breadcrumb.Ring(10);
        assertNull(ring.snapshot());
    }

    @Test
    void clearRemovesAll() {
        Breadcrumb.Ring ring = new Breadcrumb.Ring(10);
        ring.add(new Breadcrumb("a", "msg1", null));
        ring.add(new Breadcrumb("b", "msg2", null));
        ring.clear();
        assertEquals(0, ring.size());
        assertNull(ring.snapshot());
    }

    @Test
    void sizeTracksCorrectly() {
        Breadcrumb.Ring ring = new Breadcrumb.Ring(5);
        assertEquals(0, ring.size());
        ring.add(new Breadcrumb("a", "1", null));
        assertEquals(1, ring.size());
        ring.add(new Breadcrumb("b", "2", null));
        assertEquals(2, ring.size());

        // Fill and wrap
        ring.add(new Breadcrumb("c", "3", null));
        ring.add(new Breadcrumb("d", "4", null));
        ring.add(new Breadcrumb("e", "5", null));
        assertEquals(5, ring.size());
        ring.add(new Breadcrumb("f", "6", null));
        assertEquals(5, ring.size()); // still max
    }

    @Test
    void breadcrumbWithData() {
        Breadcrumb b = new Breadcrumb("http", "request", Collections.singletonMap("status", "200"));
        Map<String, Object> map = b.toMap();
        assertNotNull(map.get("timestamp"));
        assertEquals("http", map.get("category"));
        assertEquals("request", map.get("message"));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) map.get("data");
        assertNotNull(data);
        assertEquals("200", data.get("status"));
    }

    @Test
    void threadSafety() throws Exception {
        Breadcrumb.Ring ring = new Breadcrumb.Ring(100);
        int threads = 10;
        int perThread = 100;
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            workers[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    ring.add(new Breadcrumb("t" + tid, "msg" + i, null));
                }
            });
        }
        for (Thread w : workers) w.start();
        for (Thread w : workers) w.join(5000);

        // Ring should be full (capacity=100, 1000 entries added)
        assertEquals(100, ring.size());
        List<Map<String, Object>> snapshot = ring.snapshot();
        assertNotNull(snapshot);
        assertEquals(100, snapshot.size());
    }
}
