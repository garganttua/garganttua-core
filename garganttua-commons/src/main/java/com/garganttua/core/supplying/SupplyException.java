package com.garganttua.core.supplying;

public class SupplyException extends Exception {

    public SupplyException(Exception e) {
        super(e);
    }

    public SupplyException(String message) {
        super(message);
    }

}
