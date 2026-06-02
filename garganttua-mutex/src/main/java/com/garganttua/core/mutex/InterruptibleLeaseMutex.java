package com.garganttua.core.mutex;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.observability.ObservabilityEmitter;

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
 * <h2>Lease Time Management and Thread Interruption</h2>
 * <p>
 * When using strategy-based acquisition with a lease time, the execution is
 * strictly time-bounded. If the critical section execution exceeds the
 * lease time:
 * </p>
 * <ul>
 *   <li>The executing thread is <b>immediately interrupted</b> via {@link Thread#interrupt()}</li>
 *   <li>A {@link MutexException} is thrown to the calling thread indicating lease expiration</li>
 *   <li>The lock is forcefully released to prevent deadlocks</li>
 *   <li>Execution cannot continue past the lease expiration</li>
 * </ul>
 *
 * <h2>Requirements for User Code</h2>
 * <p>
 * <b>IMPORTANT:</b> Code executed within {@link #acquire(ThrowingFunction, MutexStrategy)}
 * with a lease time MUST be interruption-aware:
 * </p>
 * <ul>
 *   <li>Check {@link Thread#interrupted()} or {@link Thread#isInterrupted()} regularly</li>
 *   <li>Respond promptly to {@link InterruptedException} from blocking operations</li>
 *   <li>Do NOT swallow or ignore interruption without proper cleanup</li>
 *   <li>Avoid long-running uninterruptible operations (tight loops, heavy computation)</li>
 * </ul>
 * <p>
 * Failure to handle interruption correctly may cause the thread to continue executing
 * beyond the lease time until completion, though the lock will still be released and
 * a {@link MutexException} will still be thrown to the caller.
 * </p>
 *
 * <h2>Acquisition Strategies</h2>
 * <ul>
 * <li><b>Simple acquisition</b>: Blocks indefinitely until lock is
 * available</li>
 * <li><b>Strategy-based</b>: Configurable timeout, retries, and automatic lease
 * expiration with thread interruption</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see MutexStrategy
 */
public class InterruptibleLeaseMutex implements IMutex {
    private static final Logger log = Logger.getLogger(InterruptibleLeaseMutex.class);

    private static final String MUTEX_RELEASED_MSG = "Mutex released: {}";

    private final String name;
    private final ReentrantLock lock;
    private final ExecutorService executorService;

    /**
     * Constructs a new InterruptibleLeaseMutex with the specified name.
     *
     * @param name the unique name identifying this mutex
     */
    public InterruptibleLeaseMutex(String name) {
        this.name = name;
        this.lock = new ReentrantLock(true); // Fair lock to prevent starvation
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("mutex-lease-enforcer-" + name);
            return thread;
        });
        log.trace("InterruptibleLeaseMutex created: {}", name);
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function) throws MutexException {
        log.debug("Acquiring mutex (simple): {}", name);
        String source = "mutex:" + name;
        try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.joinCurrent()) {
            scope.fireStart(source);
            lock.lock();
            try {
                log.trace("Mutex acquired: {}", name);
                R result = function.execute();
                scope.fireEnd(source);
                return result;
            } catch (MutexException e) {
                log.warn("Mutex execution failed for {}: {}", name, e.getMessage());
                scope.fireError(source, e);
                throw e;
            } catch (Exception e) {
                log.error("Unexpected exception in mutex {}: {}", name, e.getMessage(), e);
                MutexException me = new MutexException("Unexpected exception during mutex execution", e);
                scope.fireError(source, me);
                throw me;
            } finally {
                lock.unlock();
                log.trace(MUTEX_RELEASED_MSG, name);
            }
        }
    }

    @Override
    public <R> R acquire(ThrowingFunction<R> function, MutexStrategy strategy) throws MutexException {
        log.debug("Acquiring mutex (strategy): {} with strategy: waitTime={}{}, retries={}, leaseTime={}{}",
                name,
                strategy.waitTime(),
                strategy.waitTimeUnit(),
                strategy.retries(),
                strategy.leaseTime(),
                strategy.leaseTimeUnit());

        String source = "mutex:" + name;
        int maxAttempts = 1 + strategy.retries();

        try (ObservabilityEmitter.Scope scope = ObservabilityEmitter.joinCurrent()) {
            scope.fireStart(source);
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    if (tryAcquireLock(strategy)) {
                        R result = executeWithLockAndLease(function, strategy, attempt, maxAttempts);
                        scope.fireEnd(source);
                        return result;
                    }
                    handleFailedAttempt(attempt, maxAttempts, strategy);
                } catch (InterruptedException e) {
                    MutexException me = handleInterruption(e);
                    scope.fireError(source, me);
                    throw me;
                } catch (MutexException e) {
                    log.warn("Mutex execution failed for {}: {}", name, e.getMessage());
                    scope.fireError(source, e);
                    throw e;
                } catch (Exception e) {
                    MutexException me = handleUnexpectedException(e);
                    scope.fireError(source, me);
                    throw me;
                }
            }

            MutexException me = createExhaustedException(maxAttempts);
            scope.fireError(source, me);
            throw me;
        }
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
        log.trace("Mutex acquired on attempt {}/{}: {}", attempt, maxAttempts, name);

        if (strategy.leaseTime() <= 0) {
            // No lease time enforcement, execute directly
            try {
                return function.execute();
            } finally {
                lock.unlock();
                log.trace(MUTEX_RELEASED_MSG, name);
            }
        }

        // Execute with lease time enforcement using dedicated thread
        // We need to track the execution thread to interrupt it on lease expiration
        final Thread[] executionThread = new Thread[1];
        final boolean[] leaseExpired = new boolean[1];

        Callable<R> task = () -> {
            executionThread[0] = Thread.currentThread();
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
            log.trace("Mutex execution completed within lease time: {}", name);
            return result;
        } catch (TimeoutException e) {
            leaseExpired[0] = true;

            // Interrupt the executing thread immediately
            Thread execThread = executionThread[0];
            if (execThread != null) {
                log.warn("Interrupting execution thread for mutex {} due to lease expiration", name);
                execThread.interrupt();
            }

            // Cancel the future with interrupt flag
            future.cancel(true);

            log.error("Mutex lease time exceeded for {}: {}{}. Thread interrupted and lock released.",
                    name, strategy.leaseTime(), strategy.leaseTimeUnit());
            throw new MutexException("Mutex lease time exceeded: " + strategy.leaseTime() +
                    " " + strategy.leaseTimeUnit() + ". Execution thread was interrupted.");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof MutexException mutexEx) {
                throw mutexEx;
            }
            log.error("Mutex execution failed: {}", name, cause);
            throw new MutexException("Mutex execution failed", cause);
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new MutexException("Mutex acquisition interrupted", e);
        } finally {
            // Ensure lock is always released exactly once
            lock.unlock();
            log.trace(MUTEX_RELEASED_MSG, name);
        }
    }

    private void handleFailedAttempt(int attempt, int maxAttempts, MutexStrategy strategy) throws InterruptedException {
        log.debug("Failed to acquire mutex on attempt {}/{}: {}", attempt, maxAttempts, name);
        if (attempt < maxAttempts) {
            Thread.sleep(strategy.retryIntervalUnit().toMillis(strategy.retryInterval()));
            log.trace("Retrying mutex acquisition: {}", name);
        }
    }

    private MutexException handleInterruption(InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Mutex acquisition interrupted for {}: {}", name, e.getMessage());
        return new MutexException("Mutex acquisition interrupted", e);
    }

    private MutexException handleUnexpectedException(Exception e) {
        log.error("Unexpected exception in mutex {}: {}", name, e.getMessage(), e);
        return new MutexException("Unexpected exception during mutex execution", e);
    }

    private MutexException createExhaustedException(int maxAttempts) {
        log.error("Failed to acquire mutex {} after {} attempts", name, maxAttempts);
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
