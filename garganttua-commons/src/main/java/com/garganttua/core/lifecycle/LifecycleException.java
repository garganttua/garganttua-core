package com.garganttua.core.lifecycle;

import com.garganttua.core.CoreException;

public class LifecycleException extends CoreException {

    public LifecycleException(String string) {
        super(CoreException.LIFECYCLE_ERROR, string);
    }

    public LifecycleException(Exception e) {
        super(CoreException.LIFECYCLE_ERROR, e);
    }

}
