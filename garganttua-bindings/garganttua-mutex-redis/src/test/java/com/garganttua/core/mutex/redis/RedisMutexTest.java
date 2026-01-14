package com.garganttua.core.mutex.redis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.github.siahsang.redutils.RedUtilsLock;
import org.github.siahsang.redutils.common.OperationCallBack;
import org.junit.jupiter.api.Test;

import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;

class RedisMutexTest {

    private static final String TEST_LOCK_NAME = "test-lock";

    /**
     * Stub implementation of RedUtilsLock for testing without Redis dependency.
     */
    private static class StubRedUtilsLock implements RedUtilsLock {
        private boolean shouldSucceed = true;
        private boolean shouldTrySucceed = true;
        private int acquireCallCount = 0;
        private int tryAcquireCallCount = 0;
        private RuntimeException exceptionToThrow = null;

        public void setShouldSucceed(boolean shouldSucceed) {
            this.shouldSucceed = shouldSucceed;
        }

        public void setShouldTrySucceed(boolean shouldTrySucceed) {
            this.shouldTrySucceed = shouldTrySucceed;
        }

        public void setExceptionToThrow(RuntimeException exception) {
            this.exceptionToThrow = exception;
        }

        public int getAcquireCallCount() {
            return acquireCallCount;
        }

        public int getTryAcquireCallCount() {
            return tryAcquireCallCount;
        }

        @Override
        public boolean tryAcquire(String lockName, OperationCallBack operationCallBack) {
            tryAcquireCallCount++;
            if (shouldTrySucceed) {
                operationCallBack.doOperation();
                return true;
            }
            return false;
        }

        @Override
        public void acquire(String lockName, OperationCallBack operationCallBack) {
                        acquireCallCount++;
            if (exceptionToThrow != null) {
                throw exceptionToThrow;
            }
            if (shouldSucceed) {
                operationCallBack.doOperation();
            } else {
                throw new RuntimeException("Lock acquisition failed");
            }
        }
    }

    @Test
    void testConstructorWithLockName() {
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME);
        assertNotNull(mutex);
    }

    @Test
    void testConstructorWithLockNameAndRedUtilsLock() {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        assertNotNull(mutex);
    }

    @Test
    void testAcquireExecutesFunctionSuccessfully() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        String expectedResult = "success";

        String result = mutex.acquire(() -> expectedResult);

        assertEquals(expectedResult, result);
        assertEquals(1, stub.getAcquireCallCount());
    }

    @Test
    void testAcquireWithNullFunctionThrowsException() {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);

        assertThrows(MutexException.class, () -> {
            mutex.acquire(null);
        });
    }

    @Test
    void testAcquirePropagateMutexException() {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        MutexException expectedException = new MutexException("Test exception");

        MutexException thrown = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> {
                throw expectedException;
            });
        });

        assertTrue(thrown.getMessage().contains("Test exception"));
    }

    @Test
    void testAcquireWithStrategyNull() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        String expectedResult = "result";

        String result = mutex.acquire(() -> expectedResult, null);

        assertEquals(expectedResult, result);
        assertEquals(1, stub.getAcquireCallCount());
    }

    @Test
    void testAcquireWithStrategyTryMode() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        MutexStrategy strategy = new MutexStrategy(
                0, TimeUnit.SECONDS, // waitTime = 0 means try immediately
                0, 0, TimeUnit.MILLISECONDS,
                10, TimeUnit.SECONDS);
        String expectedResult = "try-result";

        String result = mutex.acquire(() -> expectedResult, strategy);

        assertEquals(expectedResult, result);
        assertEquals(1, stub.getTryAcquireCallCount());
    }

    @Test
    void testAcquireWithStrategyTryModeFailsWhenLockNotAvailable() {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        stub.setShouldTrySucceed(false);
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        MutexStrategy strategy = new MutexStrategy(
                0, TimeUnit.SECONDS,
                0, 0, TimeUnit.MILLISECONDS,
                10, TimeUnit.SECONDS);

        assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> "result", strategy);
        });
    }

    @Test
    void testAcquireWithStrategyRetryMode() throws Exception {
        AtomicInteger attemptCount = new AtomicInteger(0);

        // Custom stub that fails first, succeeds second
        StubRedUtilsLock stub = new StubRedUtilsLock() {
            @Override
            public void acquire(String lockKey, OperationCallBack operationCallBack) {
                int attempt = attemptCount.incrementAndGet();
                if (attempt == 1) {
                    throw new RuntimeException("Lock busy");
                }
                operationCallBack.doOperation();
            }
        };

        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        MutexStrategy strategy = new MutexStrategy(
                5, TimeUnit.SECONDS,
                2, 10, TimeUnit.MILLISECONDS, // 2 retries, 10ms interval
                10, TimeUnit.SECONDS);
        String expectedResult = "retry-result";

        String result = mutex.acquire(() -> expectedResult, strategy);

        assertEquals(expectedResult, result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void testAcquireWithStrategyRetriesExhausted() {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        stub.setExceptionToThrow(new MutexException("Lock busy"));

        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        MutexStrategy strategy = new MutexStrategy(
                5, TimeUnit.SECONDS,
                2, 10, TimeUnit.MILLISECONDS,
                10, TimeUnit.SECONDS);

        MutexException thrown = assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> "result", strategy);
        });

        assertTrue(thrown.getMessage().contains("Failed to acquire lock after 3 attempts"));
        assertEquals(3, stub.getAcquireCallCount());
    }

    @Test
    void testAcquireReturnsCorrectType() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        Integer expectedResult = 42;

        Integer result = mutex.acquire(() -> expectedResult);

        assertEquals(expectedResult, result);
    }

    @Test
    void testAcquireHandlesComplexReturnType() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);

        class ComplexResult {
            String value;

            ComplexResult(String value) {
                this.value = value;
            }
        }

        ComplexResult expectedResult = new ComplexResult("test");

        ComplexResult result = mutex.acquire(() -> expectedResult);

        assertNotNull(result);
        assertEquals("test", result.value);
    }

    @Test
    void testAcquireWithNullReturnValue() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);

        String result = mutex.acquire(() -> null);

        assertNull(result);
    }

    @Test
    void testMultipleSequentialAcquires() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);

        String result1 = mutex.acquire(() -> "first");
        String result2 = mutex.acquire(() -> "second");
        String result3 = mutex.acquire(() -> "third");

        assertEquals("first", result1);
        assertEquals("second", result2);
        assertEquals("third", result3);
        assertEquals(3, stub.getAcquireCallCount());
    }

    @Test
    void testAcquireExecutesCodeWithinLock() throws Exception {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);
        AtomicBoolean codeExecuted = new AtomicBoolean(false);

        mutex.acquire(() -> {
            codeExecuted.set(true);
            return null;
        });

        assertTrue(codeExecuted.get(), "Code within acquire should have been executed");
    }

    @Test
    void testAcquireWithRuntimeExceptionFromRedUtilsLock() {
        StubRedUtilsLock stub = new StubRedUtilsLock();
        stub.setExceptionToThrow(new RuntimeException("Redis connection failed"));
        RedisMutex mutex = new RedisMutex(TEST_LOCK_NAME, stub);

        assertThrows(MutexException.class, () -> {
            mutex.acquire(() -> "result");
        });
    }

}
