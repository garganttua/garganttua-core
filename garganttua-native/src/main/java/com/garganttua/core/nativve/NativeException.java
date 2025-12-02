package com.garganttua.core.nativve;

import com.garganttua.core.CoreException;

public class NativeException extends CoreException {

    public NativeException(Throwable e) {
        super(NATIVE_ERROR, e);
    }

}
