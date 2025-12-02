package com.garganttua.core.nativve;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NativeException extends CoreException {

    public NativeException(Throwable e) {
        super(NATIVE_ERROR, e);
        log.atError().log("Native configuration error occurred: {}", e.getMessage());
    }

}
