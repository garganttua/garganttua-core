package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaseTimeTest {

    private IMutexManager manager;

    @BeforeEach
    void setUp() {
        manager = new MutexManager();
    }

    @Test
    void testLeaseTimeEnforcement() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::lease-test"));

        MutexStrategy strategy = new MutexStrategy(
                -1, TimeUnit.SECONDS,
                0, 0, TimeUnit.MILLISECONDS,
                500, TimeUnit.MILLISECONDS // 500ms lease time
        );

        // This should fail because execution takes longer than lease time
        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                try {
                    Thread.sleep(1000); // Sleep for 1 second (exceeds 500ms lease)
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new MutexException("Interrupted", e);
                }
                return "Should not complete";
            }, strategy);
        });

        assertTrue(exception.getMessage().contains("lease time exceeded"),
                "Exception should mention lease time exceeded");
    }

    @Test
    void testLeaseTimeSuccess() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::lease-success"));

        MutexStrategy strategy = new MutexStrategy(
                -1, TimeUnit.SECONDS,
                0, 0, TimeUnit.MILLISECONDS,
                1000, TimeUnit.MILLISECONDS // 1 second lease time
        );

        // This should succeed because execution completes within lease time
        String result = mutex.acquire(() -> {
            try {
                Thread.sleep(200); // Sleep for 200ms (within 1s lease)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MutexException("Interrupted", e);
            }
            return "Success";
        }, strategy);

        assertEquals("Success", result);
    }

    @Test
    void testNoLeaseTimeEnforcement() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::no-lease"));

        MutexStrategy strategy = new MutexStrategy(
                -1, TimeUnit.SECONDS,
                0, 0, TimeUnit.MILLISECONDS,
                0, TimeUnit.MILLISECONDS // No lease time (0 or negative)
        );

        // This should succeed even though it takes longer
        String result = mutex.acquire(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MutexException("Interrupted", e);
            }
            return "No lease enforcement";
        }, strategy);

        assertEquals("No lease enforcement", result);
    }

    @Test
    void testLeaseTimeForcesLockRelease() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::force-release"));
        AtomicBoolean lockAcquiredAfter = new AtomicBoolean(false);
        AtomicBoolean firstThreadStarted = new AtomicBoolean(false);

        MutexStrategy leaseStrategy = new MutexStrategy(
                -1, TimeUnit.SECONDS,
                0, 0, TimeUnit.MILLISECONDS,
                300, TimeUnit.MILLISECONDS // 300ms lease time
        );

        // First acquisition will exceed lease time
        Thread firstThread = new Thread(() -> {
            try {
                mutex.acquire(() -> {
                    firstThreadStarted.set(true);
                    try {
                        Thread.sleep(1000); // Exceeds lease time
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new MutexException("Interrupted during sleep", e);
                    }
                    return null;
                }, leaseStrategy);
            } catch (MutexException e) {
                // Expected - lease time exceeded
            }
        });
        firstThread.start();

        // Wait for first thread to acquire lock and for lease time to expire
        while (!firstThreadStarted.get()) {
            Thread.sleep(10);
        }
        Thread.sleep(400); // Wait for lease time (300ms) to expire plus buffer

        // Second acquisition should succeed because lock was forcefully released
        MutexStrategy normalStrategy = new MutexStrategy(
                100, TimeUnit.MILLISECONDS,
                3, 50, TimeUnit.MILLISECONDS,
                1000, TimeUnit.MILLISECONDS);

        String result = mutex.acquire(() -> {
            lockAcquiredAfter.set(true);
            return "Lock was released";
        }, normalStrategy);

        firstThread.join(2000);
        assertTrue(lockAcquiredAfter.get(), "Second thread should have acquired the lock");
        assertEquals("Lock was released", result);
    }

    @Test
    void testExceptionPropagationWithLease() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("InterruptibleLeaseMutex::exception-with-lease"));

        MutexStrategy strategy = new MutexStrategy(
                -1, TimeUnit.SECONDS,
                0, 0, TimeUnit.MILLISECONDS,
                1000, TimeUnit.MILLISECONDS);

        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw new MutexException("Test exception within lease");
            }, strategy);
        });

        assertTrue(exception.getMessage().contains("Test exception within lease"));
    }

}
