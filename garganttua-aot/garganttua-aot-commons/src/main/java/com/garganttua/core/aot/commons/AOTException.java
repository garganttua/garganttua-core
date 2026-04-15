package com.garganttua.core.aot.commons;

import com.garganttua.core.CoreException;

/**
 * Exception type for AOT compilation and runtime errors.
 */
public class AOTException extends CoreException {

    public static final int AOT_ERROR = 16;

    public AOTException(String message) {
        super(AOT_ERROR, message);
    }

    public AOTException(String message, Throwable cause) {
        super(AOT_ERROR, message, cause);
    }

    public AOTException(Throwable cause) {
        super(AOT_ERROR, cause);
    }

}
