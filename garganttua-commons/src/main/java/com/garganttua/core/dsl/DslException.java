package com.garganttua.core.dsl;

import com.garganttua.core.CoreException;

public class DslException extends CoreException {

    private static final long serialVersionUID = 1L;

    public DslException(String message) {
        super(CoreException.DSL_ERROR, message);
    }

    public DslException(String message, Exception cause) {
        super(CoreException.DSL_ERROR, message, cause);
    }

    public DslException(Exception e) {
        super(e);
    }

}
