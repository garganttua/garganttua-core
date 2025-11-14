package com.garganttua.core.lifecycle;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class LifecycleException extends CoreException {

    public LifecycleException(String string) {
        super(CoreExceptionCode.LIFECYCLE_ERROR, string);
    }

    public LifecycleException(Exception e) {
        super(CoreExceptionCode.LIFECYCLE_ERROR, e);
    }

}
