package io.logflux.sdk;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class QueueTest {

    @Test
    void enqueueDequeueFIFO() {
        Queue q = new Queue(10);
        q.enqueue(new Queue.Entry("msg1", 7, 1));
        q.enqueue(new Queue.Entry("msg2", 7, 1));
        q.enqueue(new Queue.Entry("msg3", 7, 1));

        assertEquals(3, q.size());
        assertFalse(q.isEmpty());

        Queue.Entry e1 = q.dequeue();
        assertNotNull(e1);
        assertEquals("msg1", e1.jsonPayload);

        Queue.Entry e2 = q.dequeue();
        assertEquals("msg2", e2.jsonPayload);

        Queue.Entry e3 = q.dequeue();
        assertEquals("msg3", e3.jsonPayload);

        assertNull(q.dequeue());
        assertTrue(q.isEmpty());
    }

    @Test
    void queueRejectsWhenFull() {
        Queue q = new Queue(3);
        assertTrue(q.enqueue(new Queue.Entry("1", 7, 1)));
        assertTrue(q.enqueue(new Queue.Entry("2", 7, 1)));
        assertTrue(q.enqueue(new Queue.Entry("3", 7, 1)));
        assertFalse(q.enqueue(new Queue.Entry("4", 7, 1)));
        assertTrue(q.isFull());
        assertEquals(3, q.size());
    }

    @Test
    void dequeueBatch() {
        Queue q = new Queue(10);
        for (int i = 0; i < 5; i++) {
            q.enqueue(new Queue.Entry("msg" + i, 7, 1));
        }

        List<Queue.Entry> batch = q.dequeueBatch(3);
        assertNotNull(batch);
        assertEquals(3, batch.size());
        assertEquals("msg0", batch.get(0).jsonPayload);
        assertEquals("msg1", batch.get(1).jsonPayload);
        assertEquals("msg2", batch.get(2).jsonPayload);
        assertEquals(2, q.size());
    }

    @Test
    void dequeueBatchMoreThanAvailable() {
        Queue q = new Queue(10);
        q.enqueue(new Queue.Entry("msg1", 7, 1));
        q.enqueue(new Queue.Entry("msg2", 7, 1));

        List<Queue.Entry> batch = q.dequeueBatch(5);
        assertNotNull(batch);
        assertEquals(2, batch.size());
        assertTrue(q.isEmpty());
    }

    @Test
    void clearRemovesAll() {
        Queue q = new Queue(10);
        q.enqueue(new Queue.Entry("msg", 7, 1));
        q.enqueue(new Queue.Entry("msg2", 7, 1));
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(0, q.size());
    }

    @Test
    void closeRejectsNewEntries() {
        Queue q = new Queue(10);
        q.enqueue(new Queue.Entry("msg", 7, 1));
        q.close();
        assertTrue(q.isClosed());
        assertFalse(q.enqueue(new Queue.Entry("msg2", 7, 1)));
        // Existing entries are still dequeueable
        assertNotNull(q.dequeue());
    }

    @Test
    void dequeueBlockingReturnsNullOnClose() throws Exception {
        Queue q = new Queue(10);
        Thread t = new Thread(() -> {
            Queue.Entry e = q.dequeueBlocking();
            assertNull(e); // should return null after close
        });
        t.start();
        Thread.sleep(100);
        q.close();
        t.join(2000);
        assertFalse(t.isAlive());
    }

    @Test
    void concurrentEnqueueDequeue() throws Exception {
        Queue q = new Queue(1000);
        int count = 500;
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger enqueued = new AtomicInteger();
        AtomicInteger dequeued = new AtomicInteger();

        Thread producer = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                if (q.enqueue(new Queue.Entry("msg" + i, 7, 1))) {
                    enqueued.incrementAndGet();
                }
            }
            latch.countDown();
        });

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < count; i++) {
                Queue.Entry e = q.dequeueBlocking();
                if (e != null) dequeued.incrementAndGet();
                if (q.isClosed() && q.isEmpty()) break;
            }
            latch.countDown();
        });

        producer.start();
        consumer.start();
        producer.join(5000);
        q.close();
        consumer.join(5000);

        assertEquals(count, enqueued.get());
        assertEquals(count, dequeued.get());
    }

    @Test
    void capacityReturnsConfiguredSize() {
        Queue q = new Queue(500);
        assertEquals(500, q.capacity());
    }

    @Test
    void defaultCapacityIs1000() {
        Queue q = new Queue(0);
        assertEquals(1000, q.capacity());
    }
}
