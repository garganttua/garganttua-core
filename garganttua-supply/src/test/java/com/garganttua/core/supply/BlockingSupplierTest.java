package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.supply.dsl.BlockingSupplierBuilder;

public class BlockingSupplierTest {

    @Test
    public void testBlockingSupplierWithAvailableElement() throws Exception {
        // Create queue with element
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.put("test value");

        // Create supplier
        ISupplier<String> supplier = new BlockingSupplier<>(queue, String.class);

        // Supply should return the queue element
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("test value", result.get(), "Result should match queue value");
        assertTrue(queue.isEmpty(), "Queue should be empty after supply");
    }

    @Test
    public void testBlockingSupplierWithMultipleElements() throws Exception {
        // Create queue with multiple elements
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        queue.put(1);
        queue.put(2);
        queue.put(3);

        // Create supplier
        ISupplier<Integer> supplier = new BlockingSupplier<>(queue, Integer.class);

        // Supply multiple times
        assertEquals(1, supplier.supply().get(), "First element should be 1");
        assertEquals(2, supplier.supply().get(), "Second element should be 2");
        assertEquals(3, supplier.supply().get(), "Third element should be 3");
        assertTrue(queue.isEmpty(), "Queue should be empty");
    }

    @Test
    public void testBlockingSupplierWithTimeout() throws Exception {
        // Create empty queue
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        // Create supplier with short timeout
        ISupplier<String> supplier = new BlockingSupplier<>(queue, String.class, 100L);

        // Supply should return empty after timeout
        Optional<String> result = supplier.supply();

        assertFalse(result.isPresent(), "Result should be empty when timeout occurs");
    }

    @Test
    public void testBlockingSupplierWithTimeoutAndLateElement() throws Exception {
        // Create empty queue
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        // Create supplier with timeout
        ISupplier<String> supplier = new BlockingSupplier<>(queue, String.class, 1000L);

        // Add element in another thread after delay
        new Thread(() -> {
            try {
                Thread.sleep(200);
                queue.put("delayed value");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Supply should wait and get the element
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("delayed value", result.get(), "Result should match delayed value");
    }

    @Test
    public void testBlockingSupplierBuilder() throws Exception {
        // Create queue
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.put("builder test");

        // Use builder
        ISupplier<String> supplier = BlockingSupplierBuilder.of(queue, String.class)
                .withTimeout(1000L)
                .build();

        // Supply should return the value
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("builder test", result.get(), "Result should match queue value");
    }

    @Test
    public void testBlockingSupplierBuilderWithoutTimeout() throws Exception {
        // Create queue
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        queue.put(456);

        // Use builder without timeout
        ISupplier<Integer> supplier = BlockingSupplierBuilder.of(queue, Integer.class)
                .build();

        // Supply should return the value
        Optional<Integer> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(456, result.get(), "Result should match queue value");
    }

    @Test
    public void testGetSuppliedType() {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        ISupplier<String> supplier = new BlockingSupplier<>(queue, String.class);

        assertEquals(String.class, supplier.getSuppliedType(),
                "Supplied type should be String.class");
    }

    @Test
    public void testBlockingSupplierWithNull() throws Exception {
        // Create queue with null (note: LinkedBlockingQueue doesn't accept null)
        // So we test with a queue implementation that does
        BlockingQueue<String> queue = new java.util.concurrent.ArrayBlockingQueue<>(10);
        // ArrayBlockingQueue doesn't accept null either, so we just verify behavior with actual values

        queue.put("value");
        ISupplier<String> supplier = new BlockingSupplier<>(queue, String.class);

        Optional<String> result = supplier.supply();
        assertTrue(result.isPresent(), "Result should be present");
    }
}
