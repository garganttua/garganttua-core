package com.garganttua.core.mutex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExceptionChainingTest {

    private IMutexManager manager;

    @BeforeEach
    void setUp() {
        manager = new MutexManager();
    }

    @Test
    void testSimpleExceptionConstructor() {
        MutexException exception = new MutexException("Test message");

        assertEquals("Test message", exception.getMessage());
        assertEquals(MutexException.MUTEX_ERROR, exception.getCode());
    }

    @Test
    void testExceptionWithCauseConstructor() {
        IOException cause = new IOException("Original IO error");
        MutexException exception = new MutexException("Wrapped exception", cause);

        assertEquals("Wrapped exception", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(MutexException.MUTEX_ERROR, exception.getCode());
    }

    @Test
    void testExceptionFromCauseConstructor() {
        IllegalStateException cause = new IllegalStateException("Invalid state");
        MutexException exception = new MutexException(cause);

        assertEquals("Invalid state", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertEquals(MutexException.MUTEX_ERROR, exception.getCode());
    }

    @Test
    void testExceptionPreservationInSimpleAcquire() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::exception-test"));
        IOException originalException = new IOException("Disk full");

        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                sneakyThrow(originalException);
                return null;
            });
        });

        assertNotNull(exception.getCause());
        assertEquals(originalException, exception.getCause());
        assertTrue(exception.getMessage().contains("Unexpected exception"));
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    @Test
    void testExceptionPreservationWithStrategy() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::strategy-exception-test"));
        MutexStrategy strategy = new MutexStrategy(
            -1, TimeUnit.SECONDS,
            0, 0, TimeUnit.MILLISECONDS,
            0, TimeUnit.MILLISECONDS
        );

        IllegalArgumentException originalException = new IllegalArgumentException("Invalid argument");

        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw originalException;
            }, strategy);
        });

        assertNotNull(exception.getCause());
        assertEquals(originalException, exception.getCause());
    }

    @Test
    void testExceptionPreservationWithLeaseTime() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::lease-exception-test"));
        MutexStrategy strategy = new MutexStrategy(
            -1, TimeUnit.SECONDS,
            0, 0, TimeUnit.MILLISECONDS,
            1000, TimeUnit.MILLISECONDS
        );

        NullPointerException originalException = new NullPointerException("Null reference");

        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw originalException;
            }, strategy);
        });

        // With lease time, the exception is wrapped in a MutexException from the Callable
        assertNotNull(exception.getCause());
        if (exception.getCause() instanceof MutexException innerException) {
            assertEquals(originalException, innerException.getCause());
        } else {
            assertEquals(originalException, exception.getCause());
        }
    }

    @Test
    void testMutexExceptionPassthrough() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::passthrough-test"));
        MutexException originalException = new MutexException("Original mutex error");

        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw originalException;
            });
        });

        assertEquals(originalException, exception);
    }

    @Test
    void testNestedExceptionChain() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::nested-exception-test"));

        IOException rootCause = new IOException("Root cause");
        IllegalStateException middleCause = new IllegalStateException("Middle", rootCause);

        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw middleCause;
            });
        });

        assertNotNull(exception.getCause());
        assertEquals(middleCause, exception.getCause());
        assertEquals(rootCause, exception.getCause().getCause());
    }

    @Test
    void testInterruptedExceptionChaining() throws Exception {
        IMutex mutex = manager.mutex(MutexName.fromString("com.garganttua.core.mutex.InterruptibleLeaseMutex::interrupted-test"));

        // Strategy with 2 second wait time
        MutexStrategy strategy = new MutexStrategy(
            2000, TimeUnit.MILLISECONDS,
            0, 0, TimeUnit.MILLISECONDS,
            0, TimeUnit.MILLISECONDS
        );

        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch startWaiting = new CountDownLatch(1);

        // First thread holds the lock
        Thread blockingThread = new Thread(() -> {
            try {
                mutex.acquire(() -> {
                    lockAcquired.countDown();
                    try {
                        // Wait until main thread starts waiting for the lock
                        startWaiting.await(5, TimeUnit.SECONDS);
                        // Then hold lock for a while
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new MutexException("Interrupted", e);
                    }
                    return null;
                });
            } catch (Exception e) {
                // Expected
            }
        });
        blockingThread.start();

        // Wait for blocking thread to acquire the lock
        lockAcquired.await(1, TimeUnit.SECONDS);

        // Thread to interrupt the main thread after a delay
        Thread currentThread = Thread.currentThread();
        Thread interrupter = new Thread(() -> {
            try {
                Thread.sleep(300);  // Give time for main thread to start waiting
                currentThread.interrupt();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        interrupter.start();

        // Small delay to ensure interrupter thread is started
        Thread.sleep(50);

        // Signal that we're about to start waiting
        startWaiting.countDown();

        // This should throw MutexException because we get interrupted while waiting for the lock
        MutexException exception = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                return "Should not complete";
            }, strategy);
        });

        // Verify the exception indicates interruption
        assertTrue(exception.getMessage().toLowerCase().contains("interrupted") ||
                   exception.getCause() instanceof InterruptedException,
                   "Exception should indicate interruption: " + exception.getMessage());

        // Clear interrupted status before cleanup
        Thread.interrupted();

        // Cleanup
        try {
            blockingThread.interrupt();
            blockingThread.join(2000);
            interrupter.join(1000);
        } catch (InterruptedException e) {
            // Ignore - we're cleaning up
            Thread.currentThread().interrupt();
        }
    }

}
