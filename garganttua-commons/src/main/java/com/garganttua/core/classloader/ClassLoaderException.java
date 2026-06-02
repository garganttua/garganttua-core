package com.garganttua.core.classloader;

import com.garganttua.core.CoreException;

/**
 * Exception thrown by {@link IClassLoaderManager} when a JAR cannot be loaded
 * or a rebuild hook fails.
 *
 * @since 2.0.0-ALPHA02
 */
public class ClassLoaderException extends CoreException {

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message the detail message explaining the failure
     */
    public ClassLoaderException(String message) {
        super(CoreException.UNKNOWN_ERROR, message);
    }

    /**
     * Constructs a new exception with the given detail message and cause.
     *
     * @param message the detail message explaining the failure
     * @param cause   the underlying cause of this exception
     */
    public ClassLoaderException(String message, Throwable cause) {
        super(CoreException.UNKNOWN_ERROR, message, cause);
    }
}
