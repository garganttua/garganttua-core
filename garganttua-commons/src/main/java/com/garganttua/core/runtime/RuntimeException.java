package com.garganttua.core.runtime;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class RuntimeException extends CoreException {

    public RuntimeException(String message) {
        super(CoreExceptionCode.RUNTIME_ERROR, message);
    }

    public RuntimeException(Exception e) {
        super(CoreExceptionCode.RUNTIME_ERROR, e);
    }

    public RuntimeException(String string, Throwable e) {
        super(CoreExceptionCode.RUNTIME_ERROR, string, e);
    }

}
