package com.garganttua.injection;

public class DiException extends Exception {

    private static final long serialVersionUID = 1L;

    public DiException() {
        super();
    }

    public DiException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiException(String msg) {
        super(msg);
    }

}
