package com.garganttua.core.injection;

import com.garganttua.core.CoreException;

public class DiException extends CoreException {

    private static final long serialVersionUID = 1L;

    public DiException(String message, Exception cause) {
        super(CoreException.INJECTION_ERROR, message, cause);
    }

     public DiException(Exception cause) {
        super(CoreException.INJECTION_ERROR, cause);
    }

    public DiException(String msg) {
        super(CoreException.INJECTION_ERROR, msg);
    }

}
