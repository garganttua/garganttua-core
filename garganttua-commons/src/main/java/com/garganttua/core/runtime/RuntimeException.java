package com.garganttua.core.runtime;

public class RuntimeException extends Exception {

    public RuntimeException(String message) {
        super(message);
    }

    public RuntimeException(Exception e) {
        super(e);
    }

}
