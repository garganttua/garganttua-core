package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SynchronizedMutexManagerTest {

    private IMutexManager manager;

    @BeforeEach
    void setUp() {
        manager = new MutexManager();
    }

    @Test
    void testMutexCreation() throws MutexException {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::test"));
        assertNotNull(mutex);
    }

    @Test
    void testSameMutexReturnedForSameName() throws MutexException {
        MutexName name = MutexName.fromString("InterruptibleLeaseMutex::test");
        IMutex mutex1 = manager.mutex(name);
        IMutex mutex2 = manager.mutex(name);
        assertSame(mutex1, mutex2);
    }

    @Test
    void testNullNameThrowsException() {
        assertThrows(NullPointerException.class, () -> manager.mutex(null));
    }

    @Test
    void testSimpleAcquisition() throws MutexException {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::counter"));
        AtomicInteger counter = new AtomicInteger(0);

        Integer result = mutex.acquire(() -> {
            counter.incrementAndGet();
            return counter.get();
        });

        assertEquals(1, result);
        assertEquals(1, counter.get());
    }

    @Test
    void testMutualExclusion() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::shared-resource"));
        int threadCount = 10;
        int incrementsPerThread = 100;
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        mutex.acquire(() -> {
                            counter.incrementAndGet();
                            return null;
                        });
                    }
                } catch (MutexException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            threads.add(thread);
            thread.start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(threadCount * incrementsPerThread, counter.get());
    }

    @Test
    void testStrategyWithTimeout() throws MutexException {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::timeout-test"));
        MutexStrategy strategy = new MutexStrategy(
            100, TimeUnit.MILLISECONDS,
            0, 0, TimeUnit.MILLISECONDS,
            1000, TimeUnit.MILLISECONDS
        );

        Integer result = mutex.acquire(() -> {
            return 42;
        }, strategy);

        assertEquals(42, result);
    }

    @Test
    void testStrategyWithRetry() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::retry-test"));
        MutexStrategy strategy = new MutexStrategy(
            0, TimeUnit.MILLISECONDS,
            3, 50, TimeUnit.MILLISECONDS,
            1000, TimeUnit.MILLISECONDS
        );

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);

        Thread blockingThread = new Thread(() -> {
            try {
                mutex.acquire(() -> {
                    lockAcquired.countDown();
                    try {
                        releaseLock.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new MutexException("Interrupted", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        blockingThread.start();

        lockAcquired.await(1, TimeUnit.SECONDS);

        Thread.sleep(100);
        releaseLock.countDown();

        Integer result = mutex.acquire(() -> {
            return 123;
        }, strategy);

        assertEquals(123, result);
        blockingThread.join();
    }

    @Test
    void testExceptionPropagation() throws MutexException {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::exception-test"));

        assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw new MutexException("Test exception");
            });
        });
    }

    @Test
    void testSupplyMethod() throws Exception {
        IMutex result = manager.supply(MutexName.fromString("InterruptibleLeaseMutex::test-mutex")).orElseThrow();
        assertNotNull(result);
    }

    @Test
    void testGetOwnerContextType() {
        assertEquals(MutexName.class, manager.getOwnerContextType());
    }

    @Test
    void testGetSuppliedType() {
        assertEquals(IMutex.class, manager.getSuppliedType());
    }

    @Test
    void testConcurrentAccessToDifferentMutexes() throws Exception {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                try {
                    IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::mutex-" + (threadId % 5)));
                    Integer result = mutex.acquire(() -> {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new MutexException("Interrupted", e);
                        }
                        return threadId;
                    });
                    results.add(result);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertEquals(threadCount, results.size());
    }

}
