package com.garganttua.core.mutex.redis;

import org.github.siahsang.redutils.RedUtilsLock;
import org.github.siahsang.redutils.RedUtilsLockImpl;
import org.github.siahsang.redutils.common.RedUtilsConfig;

import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis-based distributed mutex implementation using red-utils library.
 *
 * <p>
 * This implementation provides distributed locking capabilities across multiple
 * JVMs/processes using Redis as the coordination service.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class RedisMutex implements IMutex {

    private final RedUtilsLock redUtilsLock;
    private final String lockName;

    /**
     * Creates a Redis mutex with default configuration.
     *
     * @param lockName the unique name for this lock
     */
    public RedisMutex(String lockName) {
        this.lockName = lockName;
        this.redUtilsLock = new RedUtilsLockImpl();
        log.atDebug().log("Created RedisMutex with default config for lock: {}", lockName);
    }

    /**
     * Creates a Redis mutex with custom configuration.
     *
     * @param lockName the unique name for this lock
     * @param config   the Redis configuration
     */
    public RedisMutex(String lockName, RedUtilsConfig config) {
        this.lockName = lockName;
        this.redUtilsLock = new RedUtilsLockImpl(config);
        log.atDebug().log("Created RedisMutex with custom config for lock: {}", lockName);
    }

    /**
     * Creates a Redis mutex with custom RedUtilsLock instance.
     *
     * @param lockName     the unique name for this lock
     * @param redUtilsLock the RedUtilsLock instance to use
     */
    public RedisMutex(String lockName, RedUtilsLock redUtilsLock) {
        this.lockName = lockName;
        this.redUtilsLock = redUtilsLock;
        log.atDebug().log("Created RedisMutex with custom RedUtilsLock for lock: {}", lockName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R acquire(ThrowingFunction<R> function) throws MutexException {
        log.atTrace().log("Attempting to acquire lock: {}", lockName);

        final R[] resultHolder = (R[]) new Object[1];

        try {
            redUtilsLock.acquire(lockName, () -> {
                log.atDebug().log("Lock acquired: {}", lockName);
                try {
                    R result = function.execute();
                    log.atDebug().log("Function executed successfully in lock: {}", lockName);
                    resultHolder[0] = result;
                } catch (Exception e) {
                    log.atError().log("Function execution failed in lock: {}", lockName, e);
                    throw new MutexException("Mutex function execution failed", e);
                }
            });
            return resultHolder[0];
        } catch (MutexException e) {
            if (e.getCause() instanceof MutexException mutexException) {
                throw mutexException;
            }
            throw new MutexException("Failed to acquire or execute within mutex: " + lockName, e);
        } catch (Exception e) {
            throw new MutexException("Failed to acquire or execute within mutex: " + lockName, e);
        } finally {
            log.atTrace().log("Lock released: {}", lockName);
        }
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function, MutexStrategy strategy) throws MutexException {
        log.atTrace().log("Attempting to acquire lock with strategy: {}", lockName);

        if (strategy == null) {
            return acquire(function);
        }

        // Handle wait time strategy
        boolean tryOnly = (strategy.waitTime() == 0);

        if (tryOnly) {
            return tryAcquireWithStrategy(function);
        } else {
            return acquireWithRetries(function, strategy);
        }
    }

    /**
     * Attempts to acquire the lock immediately without waiting.
     */
    @SuppressWarnings("unchecked")
    private <R> R tryAcquireWithStrategy(ThrowingFunction<R> function) throws MutexException {
        log.atTrace().log("Trying to acquire lock immediately: {}", lockName);
        final R[] resultHolder = (R[]) new Object[1];
        try {
            boolean acquired = redUtilsLock.tryAcquire(lockName, () -> {
                log.atDebug().log("Lock acquired (try mode): {}", lockName);
                try {
                    R result = function.execute();
                    log.atDebug().log("Function executed successfully in lock (try mode): {}", lockName);
                    resultHolder[0] = result;
                } catch (MutexException e) {
                    log.atError().log("Function execution failed in lock (try mode): {}", lockName, e);
                    throw new MutexException("Mutex function execution failed", e);
                }
            });

            if (!acquired) {
                throw new MutexException("Failed to acquire lock immediately: " + lockName);
            }

            // The result is captured in the lambda but we need to return it
            // Since tryAcquire returns boolean, we need to call acquire if successful
            return resultHolder[0];

        } catch (RuntimeException e) {
            if (e.getCause() instanceof MutexException mutexException) {
                throw mutexException;
            }
            throw new MutexException("Failed to try-acquire mutex: " + lockName, e);
        } catch (Exception e) {
            throw new MutexException("Failed to try-acquire mutex: " + lockName, e);
        }
    }

    /**
     * Acquires the lock with retry logic according to the strategy.
     */
    private <R> R acquireWithRetries(ThrowingFunction<R> function, MutexStrategy strategy) throws MutexException {
        int attempts = strategy.retries() + 1; // Initial attempt + retries
        MutexException lastException = null;

        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                if (attempt > 0) {
                    long retryDelayMillis = strategy.retryIntervalUnit().toMillis(strategy.retryInterval());
                    log.atDebug().log("Retry attempt {} for lock: {} after {} ms",
                            attempt, lockName, retryDelayMillis);
                    Thread.sleep(retryDelayMillis);
                }

                return acquire(function);

            } catch (MutexException e) {
                lastException = e;
                log.atWarn().log("Attempt {} failed for lock: {}", attempt + 1, lockName, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MutexException("Interrupted while waiting to retry lock acquisition: " + lockName, e);
            }
        }

        throw new MutexException("Failed to acquire lock after " + attempts + " attempts: " + lockName, lastException);
    }
}
