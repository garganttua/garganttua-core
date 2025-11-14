package com.garganttua.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoreException extends RuntimeException {

    private static final long serialVersionUID = 7855765591949705798L;

    @Getter
    protected CoreExceptionCode code = CoreExceptionCode.UNKNOWN_ERROR;

    protected CoreException(CoreExceptionCode code, String message) {
        super(message);
        this.code = code;
    }

    protected CoreException(CoreExceptionCode code, String message, Exception exception) {
        super(message, exception);
        this.code = code;
    }

    protected CoreException(CoreExceptionCode code, Exception exception) {
        super(exception.getMessage(), exception);
        this.code = code;
    }

    protected CoreException(Exception exception) {
        super(exception.getMessage(), exception);
        if (CoreException.class.isAssignableFrom(exception.getClass())) {
            this.code = ((CoreException) exception).getCode();
        } else {
            this.code = CoreExceptionCode.UNKNOWN_ERROR;
        }
    }

    public static CoreException findFirstInException(Exception exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof CoreException) {
                return (CoreException) cause;
            }
            cause = cause.getCause();
        }
        return null;
    }

    public static void processException(Exception e) throws CoreException {
        log.atWarn().log("Error ", e);
        CoreException apiException = CoreException.findFirstInException(e);
        if (apiException != null) {
            throw apiException;
        } else {
            throw new CoreException(CoreExceptionCode.UNKNOWN_ERROR, e.getMessage(), e);
        }
    }
}
