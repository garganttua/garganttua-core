package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InterruptibleLeaseMutexFactoryTest {

    private IMutexFactory factory;

    @BeforeEach
    void setUp() {
        factory = new InterruptibleLeaseMutexFactory();
    }

    @Test
    void testCreateMutexWithValidName() throws MutexException {
        IMutex mutex = factory.createMutex("test-mutex");

        assertNotNull(mutex);
        assertTrue(mutex instanceof InterruptibleLeaseMutex);
    }

    @Test
    void testCreateMutexWithQualifiedName() throws MutexException {
        IMutex mutex = factory.createMutex("database::user-lock");

        assertNotNull(mutex);
        assertTrue(mutex instanceof InterruptibleLeaseMutex);
    }

    @Test
    void testCreateMutexReturnsNewInstanceEachTime() throws MutexException {
        IMutex mutex1 = factory.createMutex("test");
        IMutex mutex2 = factory.createMutex("test");

        assertNotNull(mutex1);
        assertNotNull(mutex2);
        assertNotSame(mutex1, mutex2, "Factory should create new instances each time");
    }

    @Test
    void testCreateMutexRejectsNullName() {
        assertThrows(NullPointerException.class, () -> {
            factory.createMutex(null);
        });
    }

    @Test
    void testCreateMutexRejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createMutex("");
        });
    }

    @Test
    void testCreateMutexRejectsWhitespaceOnlyName() {
        assertThrows(IllegalArgumentException.class, () -> {
            factory.createMutex("   ");
        });
    }

    @Test
    void testCreateMutexTrimsWhitespace() throws MutexException {
        IMutex mutex = factory.createMutex("  test-mutex  ");

        assertNotNull(mutex);
        assertTrue(mutex instanceof InterruptibleLeaseMutex);
    }

    @Test
    void testCreatedMutexIsUsable() throws MutexException {
        IMutex mutex = factory.createMutex("test-mutex");

        Integer result = mutex.acquire(() -> {
            return 42;
        });

        assertEquals(42, result);
    }

    @Test
    void testCreateMutexWithStrategy() throws MutexException {
        MutexStrategy strategy = new MutexStrategy(
            5, TimeUnit.SECONDS,
            3, 100, TimeUnit.MILLISECONDS,
            10, TimeUnit.SECONDS
        );

        IMutex mutex = factory.createMutex("test-mutex", strategy);

        assertNotNull(mutex);
        assertTrue(mutex instanceof InterruptibleLeaseMutex);
    }

    @Test
    void testCreateMutexWithStrategyRejectsNullName() {
        MutexStrategy strategy = new MutexStrategy(
            5, TimeUnit.SECONDS,
            3, 100, TimeUnit.MILLISECONDS,
            10, TimeUnit.SECONDS
        );

        assertThrows(NullPointerException.class, () -> {
            factory.createMutex(null, strategy);
        });
    }

    @Test
    void testCreateMutexWithStrategyRejectsNullStrategy() {
        assertThrows(NullPointerException.class, () -> {
            factory.createMutex("test-mutex", null);
        });
    }

    @Test
    void testCreateMutexWithStrategyRejectsEmptyName() {
        MutexStrategy strategy = new MutexStrategy(
            5, TimeUnit.SECONDS,
            3, 100, TimeUnit.MILLISECONDS,
            10, TimeUnit.SECONDS
        );

        assertThrows(IllegalArgumentException.class, () -> {
            factory.createMutex("", strategy);
        });
    }

    @Test
    void testCreatedMutexWithStrategyIsUsable() throws MutexException {
        MutexStrategy strategy = new MutexStrategy(
            -1, TimeUnit.SECONDS,
            0, 0, TimeUnit.MILLISECONDS,
            1000, TimeUnit.MILLISECONDS
        );

        IMutex mutex = factory.createMutex("test-mutex", strategy);

        // Note: Currently the strategy is not stored, so must pass it explicitly
        String result = mutex.acquire(() -> {
            return "success";
        }, strategy);

        assertEquals("success", result);
    }

    @Test
    void testMultipleMutexesAreIndependent() throws Exception {
        IMutex mutex1 = factory.createMutex("mutex-1");
        IMutex mutex2 = factory.createMutex("mutex-2");

        // Both mutexes should be acquirable simultaneously since they're independent
        boolean[] mutex1Acquired = new boolean[1];
        boolean[] mutex2Acquired = new boolean[1];

        Thread thread1 = new Thread(() -> {
            try {
                mutex1.acquire(() -> {
                    mutex1Acquired[0] = true;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                mutex2.acquire(() -> {
                    mutex2Acquired[0] = true;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return null;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        assertTrue(mutex1Acquired[0], "First mutex should have been acquired");
        assertTrue(mutex2Acquired[0], "Second mutex should have been acquired");
    }

}
