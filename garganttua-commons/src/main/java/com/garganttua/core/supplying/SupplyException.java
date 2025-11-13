package com.garganttua.core.supplying;

public class SupplyException extends RuntimeException {

    public SupplyException(Exception e) {
        super(e);
    }

    public SupplyException(String message) {
        super(message);
    }

}
