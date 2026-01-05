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
    protected MutexException(String message) {
        super(MutexException.MUTEX_ERROR, message);
        log.atTrace().log("Exiting MutexException constructor");
    }

}
