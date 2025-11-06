package com.garganttua.core.lifecycle;

public class LifecycleException extends Exception {

    public LifecycleException(String string) {
        super(string);
    }

    public LifecycleException(Exception e) {
        super(e);
    }

}
