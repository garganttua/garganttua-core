package com.garganttua.core.nativve;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.CoreException;

public class NativeException extends CoreException {
    private static final IDiagnostic log = Diagnostics.of(NativeException.class);

    public NativeException(Throwable e) {
        super(NATIVE_ERROR, e);
        log.error("Native configuration error occurred: {}", e.getMessage());
    }

}
