package com.garganttua.core.nativve;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.CoreException;

/**
 * {@link CoreException} raised when native-image configuration generation fails.
 * Carries the {@code NATIVE_ERROR} code and wraps the originating cause.
 */
public class NativeException extends CoreException {
    private static final Logger log = Logger.getLogger(NativeException.class);

    /**
     * Wraps the given cause as a native configuration error.
     *
     * @param e the underlying cause of the failure
     */
    public NativeException(Throwable e) {
        super(NATIVE_ERROR, e);
        log.error("Native configuration error occurred: {}", e.getMessage());
    }

}
