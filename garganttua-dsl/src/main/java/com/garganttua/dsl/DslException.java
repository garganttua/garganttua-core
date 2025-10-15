package com.garganttua.dsl;

public class DslException extends Exception {

    private static final long serialVersionUID = 1L;

    public DslException(String message) {
        super(message);
    }

    public DslException(String message, Throwable cause) {
        super(message, cause);
    }

}
