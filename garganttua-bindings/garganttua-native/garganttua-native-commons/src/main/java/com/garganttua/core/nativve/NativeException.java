package com.garganttua.core.nativve;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.CoreException;

public class NativeException extends CoreException {
    private static final Logger log = Logger.getLogger(NativeException.class);

    public NativeException(Throwable e) {
        super(NATIVE_ERROR, e);
        log.error("Native configuration error occurred: {}", e.getMessage());
    }

}
