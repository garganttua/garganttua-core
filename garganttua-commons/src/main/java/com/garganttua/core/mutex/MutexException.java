package com.garganttua.core.mutex;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when mutex operations fail.
 *
 * <p>
 * {@code MutexException} is the primary exception type for mutex-related errors,
 * including acquisition failures, timeout expiration, and execution errors within
 * the critical section.
 * </p>
 *
 * <h2>Common Causes</h2>
 * <ul>
 *   <li>Mutex acquisition timeout</li>
 *   <li>All retry attempts exhausted</li>
 *   <li>Execution failure within protected code</li>
 *   <li>Mutex manager unavailable</li>
 *   <li>Deadlock detection</li>
 * </ul>
 *
 * <h2>Exception Chaining</h2>
 * <p>
 * MutexException supports exception chaining to preserve the original cause
 * of mutex-related errors. This is particularly useful when wrapping exceptions
 * thrown from critical sections or during mutex acquisition.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple exception
 * throw new MutexException("Failed to acquire mutex");
 *
 * // Exception with cause
 * try {
 *     // Critical section code
 * } catch (IOException e) {
 *     throw new MutexException("IO error during mutex execution", e);
 * }
 *
 * // Re-throwing with additional context
 * try {
 *     mutex.acquire(() -> { ... });
 * } catch (Exception e) {
 *     throw new MutexException(e);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutex
 * @see IMutexManager
 */
@Slf4j
public class MutexException extends CoreException{

    /**
     * Constructs a new MutexException with the specified message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public MutexException(String message) {
        super(MutexException.MUTEX_ERROR, message);
        log.atTrace().log("MutexException created with message: {}", message);
    }

    /**
     * Constructs a new MutexException with the specified message and cause.
     *
     * <p>
     * This constructor is useful for wrapping exceptions that occur during
     * mutex operations while providing additional context.
     * </p>
     *
     * @param message the detail message explaining the error
     * @param cause the underlying cause of this exception
     */
    public MutexException(String message, Throwable cause) {
        super(MutexException.MUTEX_ERROR, message, cause);
        log.atTrace().log("MutexException created with message: {} and cause: {}", message, cause.getClass().getSimpleName());
    }

    /**
     * Constructs a new MutexException wrapping another exception.
     *
     * <p>
     * This constructor is useful for re-throwing exceptions that occur during
     * mutex operations. The message from the wrapped exception is preserved.
     * </p>
     *
     * @param cause the exception to wrap
     */
    public MutexException(Throwable cause) {
        super(MutexException.MUTEX_ERROR, cause);
        log.atTrace().log("MutexException created from cause: {}", cause.getClass().getSimpleName());
    }

}
