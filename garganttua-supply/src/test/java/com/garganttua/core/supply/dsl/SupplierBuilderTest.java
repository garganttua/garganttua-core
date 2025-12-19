package com.garganttua.core.supply.dsl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

public class SupplierBuilderTest {

    @Test
    public void testSupplierBuilderWithFuture() throws Exception {
        // Create a completed future
        CompletableFuture<String> future = CompletableFuture.completedFuture("test value");

        // Build supplier using withFuture
        ISupplier<String> supplier = new SupplierBuilder<>(String.class)
                .withFuture(future)
                .build();

        // Supply should return the future's value
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("test value", result.get(), "Result should match future value");
    }

    @Test
    public void testSupplierBuilderWithFutureAndTimeout() throws Exception {
        // Create an async future
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 42;
        });

        // Build supplier using withFuture with timeout
        ISupplier<Integer> supplier = new SupplierBuilder<>(Integer.class)
                .withFuture(future, 5000L)
                .build();

        // Supply should wait and return the value
        Optional<Integer> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(42, result.get(), "Result should match future value");
    }

    @Test
    public void testSupplierBuilderWithFutureTimeout() {
        // Create a future that never completes
        CompletableFuture<String> future = new CompletableFuture<>();

        // Build supplier with short timeout
        ISupplier<String> supplier = new SupplierBuilder<>(String.class)
                .withFuture(future, 100L)
                .build();

        // Supply should throw SupplyException due to timeout
        assertThrows(SupplyException.class, () -> supplier.supply(),
                "Should throw SupplyException on timeout");
    }

    @Test
    public void testSupplierBuilderWithBlockingQueue() throws Exception {
        // Create queue with element
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        queue.put("queue value");

        // Build supplier using withBlockingQueue
        ISupplier<String> supplier = new SupplierBuilder<>(String.class)
                .withBlockingQueue(queue)
                .build();

        // Supply should return the queue element
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("queue value", result.get(), "Result should match queue value");
        assertTrue(queue.isEmpty(), "Queue should be empty after supply");
    }

    @Test
    public void testSupplierBuilderWithBlockingQueueAndTimeout() throws Exception {
        // Create queue with element
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        queue.put(123);

        // Build supplier using withBlockingQueue with timeout
        ISupplier<Integer> supplier = new SupplierBuilder<>(Integer.class)
                .withBlockingQueue(queue, 1000L)
                .build();

        // Supply should return the queue element
        Optional<Integer> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(123, result.get(), "Result should match queue value");
    }

    @Test
    public void testSupplierBuilderWithBlockingQueueTimeout() throws Exception {
        // Create empty queue
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        // Build supplier with short timeout and nullable enabled
        ISupplier<String> supplier = new SupplierBuilder<>(String.class)
                .withBlockingQueue(queue, 100L)
                .nullable(true)
                .build();

        // Supply should return empty after timeout
        Optional<String> result = supplier.supply();

        assertFalse(result.isPresent(), "Result should be empty when timeout occurs");
    }

    @Test
    public void testSupplierBuilderWithFutureNullable() throws Exception {
        // Create a future that returns null
        CompletableFuture<String> future = CompletableFuture.completedFuture(null);

        // Build supplier with nullable enabled
        ISupplier<String> supplier = new SupplierBuilder<>(String.class)
                .withFuture(future)
                .nullable(true)
                .build();

        // Supply should return empty Optional
        Optional<String> result = supplier.supply();

        assertFalse(result.isPresent(), "Result should be empty for null value");
    }

    @Test
    public void testSupplierBuilderWithBlockingQueueNullable() throws Exception {
        // Create queue (note: standard queues don't accept null)
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        // Build supplier with short timeout and nullable
        ISupplier<String> supplier = new SupplierBuilder<>(String.class)
                .withBlockingQueue(queue, 50L)
                .nullable(true)
                .build();

        // Supply should return empty after timeout
        Optional<String> result = supplier.supply();

        assertFalse(result.isPresent(), "Result should be empty when timeout occurs");
    }
}
