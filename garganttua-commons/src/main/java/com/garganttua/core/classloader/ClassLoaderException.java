package com.garganttua.core.classloader;

import com.garganttua.core.CoreException;

/**
 * Exception thrown by {@link IClassLoaderManager} when a JAR cannot be loaded
 * or a rebuild hook fails.
 *
 * @since 2.0.0-ALPHA02
 */
public class ClassLoaderException extends CoreException {

    public ClassLoaderException(String message) {
        super(CoreException.UNKNOWN_ERROR, message);
    }

    public ClassLoaderException(String message, Throwable cause) {
        super(CoreException.UNKNOWN_ERROR, message, cause);
    }
}
