package com.garganttua.core.mutex;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import lombok.extern.slf4j.Slf4j;

/**
 * Mutex implementation using Java's {@link ReentrantLock} for thread-safe
 * critical section execution.
 *
 * <p>
 * This implementation provides mutual exclusion using a {@link ReentrantLock},
 * supporting
 * both simple acquisition (wait forever) and strategy-based acquisition with
 * timeout,
 * retry logic, and lease time management.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The {@link ReentrantLock} ensures that only one thread can execute within the
 * protected
 * critical section at a time. The lock is automatically released after
 * execution completes,
 * even if an exception is thrown.
 * </p>
 *
 * <h2>Lease Time Management</h2>
 * <p>
 * When using strategy-based acquisition with a lease time, the execution is
 * wrapped in a
 * {@link Future} with a timeout. If the critical section execution exceeds the
 * lease time,
 * a {@link MutexException} is thrown and the lock is forcefully released to
 * prevent deadlocks.
 * </p>
 *
 * <h2>Acquisition Strategies</h2>
 * <ul>
 * <li><b>Simple acquisition</b>: Blocks indefinitely until lock is
 * available</li>
 * <li><b>Strategy-based</b>: Configurable timeout, retries, and automatic lease
 * expiration</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see MutexStrategy
 */
@Slf4j
public class SynchronizedMutex implements IMutex {

    private static final String MUTEX_RELEASED_MSG = "Mutex released: {}";

    private final String name;
    private final ReentrantLock lock;
    private final ExecutorService executorService;

    /**
     * Constructs a new SynchronizedMutex with the specified name.
     *
     * @param name the unique name identifying this mutex
     */
    public SynchronizedMutex(String name) {
        this.name = name;
        this.lock = new ReentrantLock(true); // Fair lock to prevent starvation
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("mutex-lease-enforcer-" + name);
            return thread;
        });
        log.atTrace().log("SynchronizedMutex created: {}", name);
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function) throws MutexException {
        log.atDebug().log("Acquiring mutex (simple): {}", name);
        lock.lock();
        try {
            log.atTrace().log("Mutex acquired: {}", name);
            return function.execute();
        } catch (MutexException e) {
            log.atWarn().log("Mutex execution failed for {}: {}", name, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.atError().log("Unexpected exception in mutex {}: {}", name, e.getMessage(), e);
            throw new MutexException("Unexpected exception during mutex execution", e);
        } finally {
            lock.unlock();
            log.atTrace().log(MUTEX_RELEASED_MSG, name);
        }
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function, MutexStrategy strategy) throws MutexException {
        log.atDebug().log("Acquiring mutex (strategy): {} with strategy: waitTime={}{}, retries={}, leaseTime={}{}",
                name,
                strategy.waitTime(),
                strategy.waitTimeUnit(),
                strategy.retries(),
                strategy.leaseTime(),
                strategy.leaseTimeUnit());

        int maxAttempts = 1 + strategy.retries();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (tryAcquireLock(strategy)) {
                    return executeWithLockAndLease(function, strategy, attempt, maxAttempts);
                }
                handleFailedAttempt(attempt, maxAttempts, strategy);
            } catch (InterruptedException e) {
                throw handleInterruption(e);
            } catch (MutexException e) {
                log.atWarn().log("Mutex execution failed for {}: {}", name, e.getMessage());
                throw e;
            } catch (Exception e) {
                throw handleUnexpectedException(e);
            }
        }

        throw createExhaustedException(maxAttempts);
    }

    private boolean tryAcquireLock(MutexStrategy strategy) throws InterruptedException {
        if (strategy.waitTime() < 0) {
            lock.lock();
            return true;
        }
        if (strategy.waitTime() == 0) {
            return lock.tryLock();
        }
        return lock.tryLock(strategy.waitTime(), strategy.waitTimeUnit());
    }

    private <R> R executeWithLockAndLease(ThrowingFunction<R> function, MutexStrategy strategy,
            int attempt, int maxAttempts) throws MutexException {
        log.atTrace().log("Mutex acquired on attempt {}/{}: {}", attempt, maxAttempts, name);

        if (strategy.leaseTime() <= 0) {
            // No lease time enforcement, execute directly
            try {
                return function.execute();
            } finally {
                lock.unlock();
                log.atTrace().log(MUTEX_RELEASED_MSG, name);
            }
        }

        // Execute with lease time enforcement
        Callable<R> task = () -> {
            try {
                return function.execute();
            } catch (MutexException e) {
                throw e;
            } catch (Exception e) {
                throw new MutexException("Exception during mutex execution", e);
            }
        };

        Future<R> future = executorService.submit(task);

        try {
            R result = future.get(strategy.leaseTime(), strategy.leaseTimeUnit());
            log.atTrace().log("Mutex execution completed within lease time: {}", name);
            return result;
        } catch (TimeoutException e) {
            future.cancel(true);
            log.atError().log("Mutex lease time exceeded for {}: {}{}. Forcing lock release.",
                    name, strategy.leaseTime(), strategy.leaseTimeUnit());
            throw new MutexException("Mutex lease time exceeded: " + strategy.leaseTime() +
                    " " + strategy.leaseTimeUnit());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MutexException mutexEx) {
                throw mutexEx;
            }
            log.atError().log("Mutex execution failed: {}", name, cause);
            throw new MutexException("Mutex execution failed", cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new MutexException("Mutex execution interrupted", e);
        } finally {
            lock.unlock();
            log.atTrace().log(MUTEX_RELEASED_MSG, name);
        }
    }

    private void handleFailedAttempt(int attempt, int maxAttempts, MutexStrategy strategy) throws InterruptedException {
        log.atDebug().log("Failed to acquire mutex on attempt {}/{}: {}", attempt, maxAttempts, name);
        if (attempt < maxAttempts) {
            Thread.sleep(strategy.retryIntervalUnit().toMillis(strategy.retryInterval()));
            log.atTrace().log("Retrying mutex acquisition: {}", name);
        }
    }

    private MutexException handleInterruption(InterruptedException e) {
        Thread.currentThread().interrupt();
        log.atWarn().log("Mutex acquisition interrupted for {}: {}", name, e.getMessage());
        return new MutexException("Mutex acquisition interrupted", e);
    }

    private MutexException handleUnexpectedException(Exception e) {
        log.atError().log("Unexpected exception in mutex {}: {}", name, e.getMessage(), e);
        return new MutexException("Unexpected exception during mutex execution", e);
    }

    private MutexException createExhaustedException(int maxAttempts) {
        log.atError().log("Failed to acquire mutex {} after {} attempts", name, maxAttempts);
        return new MutexException("Failed to acquire mutex '" + name + "' after " + maxAttempts + " attempts");
    }

    /**
     * Returns the name of this mutex.
     *
     * @return the mutex name
     */
    public String getName() {
        return name;
    }

}
