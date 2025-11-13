package com.garganttua.core.dsl;

public class DslException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DslException(String message) {
        super(message);
    }

    public DslException(String message, Exception cause) {
        super(message, cause);
    }

    public DslException(Exception e) {
        super(e);
    }

}
