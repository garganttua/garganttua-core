package com.garganttua.core.supply;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.garganttua.core.supply.dsl.FutureSupplierBuilder;

public class FutureSupplierTest {

    @Test
    public void testFutureSupplierWithSuccessfulFuture() throws Exception {
        // Create a future that completes successfully
        CompletableFuture<String> future = CompletableFuture.completedFuture("test value");

        // Create supplier
        ISupplier<String> supplier = new FutureSupplier<>(future, String.class);

        // Supply should return the future's value
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("test value", result.get(), "Result should match future value");
    }

    @Test
    public void testFutureSupplierWithAsyncFuture() throws Exception {
        // Create a future that completes asynchronously
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return 42;
        });

        // Create supplier with timeout
        ISupplier<Integer> supplier = new FutureSupplier<>(future, Integer.class, 5000L);

        // Supply should wait and return the value
        Optional<Integer> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(42, result.get(), "Result should match future value");
    }

    @Test
    public void testFutureSupplierWithTimeout() {
        // Create a future that never completes
        CompletableFuture<String> future = new CompletableFuture<>();

        // Create supplier with short timeout
        ISupplier<String> supplier = new FutureSupplier<>(future, String.class, 100L);

        // Supply should throw SupplyException due to timeout
        assertThrows(SupplyException.class, () -> supplier.supply(),
                "Should throw SupplyException on timeout");
    }

    @Test
    public void testFutureSupplierWithFailedFuture() {
        // Create a future that completes exceptionally
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Future failed"));

        // Create supplier
        ISupplier<String> supplier = new FutureSupplier<>(future, String.class);

        // Supply should throw SupplyException
        assertThrows(SupplyException.class, () -> supplier.supply(),
                "Should throw SupplyException when future fails");
    }

    @Test
    public void testFutureSupplierWithNull() throws Exception {
        // Create a future that returns null
        CompletableFuture<String> future = CompletableFuture.completedFuture(null);

        // Create supplier
        ISupplier<String> supplier = new FutureSupplier<>(future, String.class);

        // Supply should return empty Optional
        Optional<String> result = supplier.supply();

        assertFalse(result.isPresent(), "Result should be empty for null value");
    }

    @Test
    public void testFutureSupplierBuilder() throws Exception {
        // Create a future
        CompletableFuture<String> future = CompletableFuture.completedFuture("builder test");

        // Use builder
        ISupplier<String> supplier = FutureSupplierBuilder.of(future, String.class)
                .withTimeout(1000L)
                .build();

        // Supply should return the value
        Optional<String> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals("builder test", result.get(), "Result should match future value");
    }

    @Test
    public void testFutureSupplierBuilderWithoutTimeout() throws Exception {
        // Create a future
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(123);

        // Use builder without timeout
        ISupplier<Integer> supplier = FutureSupplierBuilder.of(future, Integer.class)
                .build();

        // Supply should return the value
        Optional<Integer> result = supplier.supply();

        assertTrue(result.isPresent(), "Result should be present");
        assertEquals(123, result.get(), "Result should match future value");
    }

    @Test
    public void testGetSuppliedType() {
        CompletableFuture<String> future = CompletableFuture.completedFuture("test");
        ISupplier<String> supplier = new FutureSupplier<>(future, String.class);

        assertEquals(String.class, supplier.getSuppliedType(),
                "Supplied type should be String.class");
    }
}
