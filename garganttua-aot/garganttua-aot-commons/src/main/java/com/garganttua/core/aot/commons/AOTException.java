package com.garganttua.core.aot.commons;

import com.garganttua.core.CoreException;

/**
 * Exception type for AOT compilation and runtime errors.
 */
public class AOTException extends CoreException {

    /** {@link CoreException} error code carried by every {@code AOTException}. */
    public static final int AOT_ERROR = 16;

    /**
     * Creates an exception with the given message.
     *
     * @param message the detail message
     */
    public AOTException(String message) {
        super(AOT_ERROR, message);
    }

    /**
     * Creates an exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public AOTException(String message, Throwable cause) {
        super(AOT_ERROR, message, cause);
    }

    /**
     * Creates an exception wrapping the given cause.
     *
     * @param cause the underlying cause
     */
    public AOTException(Throwable cause) {
        super(AOT_ERROR, cause);
    }

}
