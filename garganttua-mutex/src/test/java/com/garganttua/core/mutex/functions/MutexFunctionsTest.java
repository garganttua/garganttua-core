package com.garganttua.core.mutex.functions;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.MutexManager;
import com.garganttua.core.mutex.context.MutexContext;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Tests for MutexFunctions.
 */
public class MutexFunctionsTest {

    private IMutexManager mutexManager;

    @BeforeEach
    void setUp() {
        mutexManager = new MutexManager();
        MutexContext.set(mutexManager);
    }

    @AfterEach
    void tearDown() {
        MutexContext.clear();
    }

    // Helper method to create a simple supplier
    private <T> ISupplier<T> supplierOf(T value) {
        return new ISupplier<T>() {
            @Override
            public Optional<T> supply() throws SupplyException {
                return Optional.ofNullable(value);
            }

            @Override
            public Type getSuppliedType() {
                return value != null ? value.getClass() : Object.class;
            }
        };
    }

    // Helper method to create a supplier with a callable
    private <T> ISupplier<T> supplierOf(java.util.concurrent.Callable<T> callable) {
        return new ISupplier<T>() {
            @Override
            public Optional<T> supply() throws SupplyException {
                try {
                    return Optional.ofNullable(callable.call());
                } catch (Exception e) {
                    throw new SupplyException(e);
                }
            }

            @Override
            public Type getSuppliedType() {
                return Object.class;
            }
        };
    }

    @Test
    void testSyncExecutesExpression() {
        // Simple test - sync should execute the expression and return result
        Object result = MutexFunctions.sync("test-lock", supplierOf("hello"));

        assertEquals("hello", result);
    }

    @Test
    void testSyncWithNullMutexNameThrows() {
        assertThrows(ExpressionException.class, () -> {
            MutexFunctions.sync((String) null, supplierOf("value"));
        });
    }

    @Test
    void testSyncWithBlankMutexNameThrows() {
        assertThrows(ExpressionException.class, () -> {
            MutexFunctions.sync("   ", supplierOf("value"));
        });
    }

    @Test
    void testSyncWithNullExpressionThrows() {
        assertThrows(ExpressionException.class, () -> {
            MutexFunctions.sync("lock", null);
        });
    }

    @Test
    void testSyncWithNoContextThrows() {
        MutexContext.clear();

        assertThrows(ExpressionException.class, () -> {
            MutexFunctions.sync("lock", supplierOf("value"));
        });
    }

    @Test
    void testSyncProvidesExclusiveAccess() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        // Note: MutexContext is thread-local, so we need to set it in each thread
        // or use a shared approach. For this test, we set the same manager in each thread.
        IMutexManager sharedManager = mutexManager;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                // Set the context for this thread
                MutexContext.set(sharedManager);
                try {
                    startLatch.await();
                    MutexFunctions.sync("exclusive-lock", supplierOf(() -> {
                        // Increment counter
                        int current = counter.incrementAndGet();
                        // Track max concurrent
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));
                        // Simulate some work
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        // Decrement counter
                        counter.decrementAndGet();
                        return null;
                    }));
                } catch (Exception e) {
                    fail("Unexpected exception: " + e);
                } finally {
                    MutexContext.clear();
                    doneLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Threads should complete within timeout");

        executor.shutdown();

        // Max concurrent should be 1 (exclusive access)
        assertEquals(1, maxConcurrent.get(), "Only one thread should access the critical section at a time");
    }

    @Test
    void testSyncWithObjectMutexName() {
        // Test the Object overload
        Object lockName = "object-lock";
        Object result = MutexFunctions.sync(lockName, supplierOf(42));

        assertEquals(42, result);
    }

    @Test
    void testSyncReturnsNull() {
        // Test that sync can return null from expression
        Object result = MutexFunctions.sync("null-lock", supplierOf((Object) null));

        assertNull(result);
    }

    @Test
    void testSyncWrapsExceptionProperly() {
        // Test that exceptions from the expression are properly wrapped
        ExpressionException thrown = assertThrows(ExpressionException.class, () -> {
            MutexFunctions.sync("error-lock", supplierOf(() -> {
                throw new RuntimeException("Test error");
            }));
        });

        // The exception message should start with "sync:"
        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().startsWith("sync:"),
                "Exception message should start with 'sync:' but was: " + thrown.getMessage());
    }
}
