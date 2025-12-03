package com.garganttua.core.runtime;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MutexException extends CoreException{

    protected MutexException(int code, String message) {
        super(code, message);
        log.atTrace().log("Exiting MutexException constructor");
    }

}
