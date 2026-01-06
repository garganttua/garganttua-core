package com.garganttua.core.mutex;

import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.MutexException;
import com.garganttua.core.mutex.MutexStrategy;

/**
 * Mutex interface for thread-safe critical section execution.
 *
 * <p>
 * {@code IMutex} provides a mechanism to execute code within a mutually exclusive
 * lock, ensuring that only one thread can execute the protected code at a time.
 * It supports both simple acquisition (wait forever) and strategy-based acquisition
 * with configurable timeout, retries, and lease time.
 * </p>
 *
 * <h2>Usage Patterns</h2>
 * <ul>
 *   <li>Database transaction serialization</li>
 *   <li>Shared resource access control</li>
 *   <li>Distributed lock coordination</li>
 *   <li>Cache invalidation synchronization</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see MutexStrategy
 * @see IMutexManager
 */
public interface IMutex {

    /**
     * Functional interface for code to be executed within mutex protection.
     *
     * <p>
     * This interface allows throwing {@link MutexException} from the protected code,
     * enabling proper error propagation from the critical section.
     * </p>
     *
     * @param <R> the return type of the function
     */
    @FunctionalInterface
    public interface ThrowingFunction<R> {
        /**
         * Executes the protected code within the mutex.
         *
         * @return the result of the execution
         * @throws MutexException if execution fails
         */
        R execute() throws MutexException;
    }

    /**
     * Acquires the mutex and executes the function, waiting indefinitely if necessary.
     *
     * <p>
     * This method will block until the mutex becomes available. Once acquired,
     * the function is executed and the mutex is automatically released upon completion.
     * </p>
     *
     * @param <R> the return type of the function
     * @param function the function to execute within the mutex
     * @return the result of the function execution
     * @throws MutexException if mutex acquisition or function execution fails
     */
    <R> R acquire(ThrowingFunction<R> function) throws MutexException;

    /**
     * Acquires the mutex and executes the function using the specified strategy.
     *
     * <p>
     * This method provides fine-grained control over mutex acquisition behavior
     * through the {@link MutexStrategy}, including:
     * </p>
     * <ul>
     *   <li>Wait timeout configuration</li>
     *   <li>Retry attempts and intervals</li>
     *   <li>Lease time for automatic release</li>
     * </ul>
     *
     * @param <R> the return type of the function
     * @param function the function to execute within the mutex
     * @param strategy the acquisition strategy defining timeout, retries, and lease time
     * @return the result of the function execution
     * @throws MutexException if mutex acquisition fails or function execution fails
     * @see MutexStrategy
     */
    <R> R acquire(ThrowingFunction<R> function, MutexStrategy strategy) throws MutexException;

}
