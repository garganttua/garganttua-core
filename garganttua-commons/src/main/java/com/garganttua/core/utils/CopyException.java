package com.garganttua.core.utils;

import com.garganttua.core.CoreException;

public class CopyException extends CoreException {

    public CopyException(String string) {
        super(CoreException.COPY_ERROR, string);
    }

    public CopyException(Exception e) {
        super(CoreException.COPY_ERROR, e);
    }

}
