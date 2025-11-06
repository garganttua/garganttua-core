package com.garganttua.core.injection;

public class DiException extends Exception {

    private static final long serialVersionUID = 1L;

    public DiException() {
        super();
    }

    public DiException(String message, Exception cause) {
        super(message, cause);
    }

     public DiException(Exception cause) {
        super(cause);
    }

    public DiException(String msg) {
        super(msg);
    }

}
