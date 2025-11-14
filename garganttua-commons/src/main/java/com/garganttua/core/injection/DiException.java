package com.garganttua.core.injection;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class DiException extends CoreException {

    private static final long serialVersionUID = 1L;

    public DiException(String message, Exception cause) {
        super(CoreExceptionCode.INJECTION_ERROR, message, cause);
    }

     public DiException(Exception cause) {
        super(CoreExceptionCode.INJECTION_ERROR, cause);
    }

    public DiException(String msg) {
        super(CoreExceptionCode.INJECTION_ERROR, msg);
    }

}
