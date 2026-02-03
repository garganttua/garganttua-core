package com.garganttua.core.script;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.InterruptibleLeaseMutex;
import com.garganttua.core.mutex.MutexStrategy;
import com.garganttua.core.script.functions.ScriptFunctions;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.reflections.ReflectionsAnnotationScanner;

class ScriptSynchronizedTest {

    @BeforeAll
    static void setup() {
        ObjectReflectionHelper.setAnnotationScanner(new ReflectionsAnnotationScanner());
    }

    // ---- Basic synchronized execution ----

    @Test
    void testSynchronizedWithDirectValue() {
        IMutex mutex = new InterruptibleLeaseMutex("test-mutex");
        Object result = ScriptFunctions.synchronizedExec("test", mutex, "acquire", 1000, "hello");
        assertEquals("hello", result);
    }

    @Test
    void testSynchronizedWithNullExpression() {
        IMutex mutex = new InterruptibleLeaseMutex("test-mutex");
        Object result = ScriptFunctions.synchronizedExec("test", mutex, "acquire", 1000, null);
        assertNull(result);
    }

    // ---- Sync (simplified) function ----

    @Test
    void testSyncWithDirectValue() {
        IMutex mutex = new InterruptibleLeaseMutex("test-sync");
        Object result = ScriptFunctions.sync("test", mutex, "sync-value");
        assertEquals("sync-value", result);
    }

    @Test
    void testSyncWithNullExpression() {
        IMutex mutex = new InterruptibleLeaseMutex("test-sync");
        Object result = ScriptFunctions.sync("test", mutex, null);
        assertNull(result);
    }

    // ---- Mode validation ----

    @Test
    void testSynchronizedAcquireMode() {
        IMutex mutex = new InterruptibleLeaseMutex("test-acquire");
        Object result = ScriptFunctions.synchronizedExec("test", mutex, "acquire", 100, "acquired");
        assertEquals("acquired", result);
    }

    @Test
    void testSynchronizedTryAcquireMode() {
        IMutex mutex = new InterruptibleLeaseMutex("test-tryacquire");
        Object result = ScriptFunctions.synchronizedExec("test", mutex, "tryAcquire", 0, "tryacquired");
        assertEquals("tryacquired", result);
    }

    @Test
    void testSynchronizedInvalidMode() {
        IMutex mutex = new InterruptibleLeaseMutex("test-invalid");
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.synchronizedExec("test", mutex, "invalid", 100, "value"));
    }

    // ---- Parameter validation ----

    @Test
    void testSynchronizedNullMutexName() {
        IMutex mutex = new InterruptibleLeaseMutex("test");
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.synchronizedExec(null, mutex, "acquire", 100, "value"));
    }

    @Test
    void testSynchronizedBlankMutexName() {
        IMutex mutex = new InterruptibleLeaseMutex("test");
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.synchronizedExec("", mutex, "acquire", 100, "value"));
    }

    @Test
    void testSynchronizedNullMutex() {
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.synchronizedExec("test", null, "acquire", 100, "value"));
    }

    @Test
    void testSynchronizedNullMode() {
        IMutex mutex = new InterruptibleLeaseMutex("test");
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.synchronizedExec("test", mutex, null, 100, "value"));
    }

    @Test
    void testSyncNullMutexName() {
        IMutex mutex = new InterruptibleLeaseMutex("test");
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.sync(null, mutex, "value"));
    }

    @Test
    void testSyncNullMutex() {
        assertThrows(ExpressionException.class, () ->
                ScriptFunctions.sync("test", null, "value"));
    }

    // ---- Concurrency test using IMutex directly ----

    @Test
    void testSynchronizedMutualExclusion() throws InterruptedException {
        IMutex mutex = new InterruptibleLeaseMutex("concurrency-test");
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Use the mutex directly to test mutual exclusion
                    mutex.acquire(() -> {
                        int current = currentConcurrent.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));
                        counter.incrementAndGet();
                        try {
                            Thread.sleep(10); // Simulate work
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        currentConcurrent.decrementAndGet();
                        return null;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    // Ignore
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(threadCount, counter.get());
        // Max concurrent should be 1 (mutex ensures mutual exclusion)
        assertEquals(1, maxConcurrent.get());
    }

    // ---- Test synchronized function with ISupplier ----

    @Test
    void testSynchronizedWithSupplier() {
        IMutex mutex = new InterruptibleLeaseMutex("supplier-test");
        AtomicInteger callCount = new AtomicInteger(0);

        // Create a supplier that increments counter (ISupplier is not a functional interface)
        com.garganttua.core.supply.ISupplier<String> supplier = new com.garganttua.core.supply.ISupplier<String>() {
            @Override
            public java.util.Optional<String> supply() {
                callCount.incrementAndGet();
                return java.util.Optional.of("result");
            }

            @Override
            public java.lang.reflect.Type getSuppliedType() {
                return String.class;
            }
        };

        Object result = ScriptFunctions.synchronizedExec("test", mutex, "acquire", 1000, supplier);
        assertEquals("result", result);
        assertEquals(1, callCount.get());
    }
}
