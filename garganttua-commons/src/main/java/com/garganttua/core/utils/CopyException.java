package com.garganttua.core.utils;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class CopyException extends CoreException {

    public CopyException(String string) {
        super(CoreExceptionCode.COPY_ERROR, string);
    }

    public CopyException(Exception e) {
        super(CoreExceptionCode.COPY_ERROR, e);
    }

}
