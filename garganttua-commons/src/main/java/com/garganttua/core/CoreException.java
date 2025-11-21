package com.garganttua.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

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

    protected CoreException(CoreExceptionCode code, String message, Throwable exception) {
        super(message, exception);
        this.code = code;
    }

    protected CoreException(CoreExceptionCode code, Throwable exception) {
        super(exception.getMessage(), exception);
        this.code = code;
    }

    protected CoreException(Throwable exception) {
        super(exception.getMessage(), exception);
        if (CoreException.class.isAssignableFrom(exception.getClass())) {
            this.code = ((CoreException) exception).getCode();
        } else {
            this.code = CoreExceptionCode.UNKNOWN_ERROR;
        }
    }

    public static Optional<CoreException> findFirstInException(Throwable exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (CoreException.class.isAssignableFrom(cause.getClass()) ) {
                return Optional.of((CoreException) cause);
            }
            cause = cause.getCause();
        }
        return Optional.empty();
    }

    public static void processException(Throwable e) throws CoreException {
        log.atWarn().log("Error ", e);
        Optional<CoreException> coreException = CoreException.findFirstInException(e);
        if (coreException.isPresent()) {
            throw coreException.get();
        } else {
            throw new CoreException(CoreExceptionCode.UNKNOWN_ERROR, e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Optional<E> findFirstInException(Throwable exception,
            Class<E> type) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause.getClass().isAssignableFrom(type) ) {
                return Optional.of((E) cause);
            }
            if( cause instanceof InvocationTargetException inv ){
                cause = inv.getTargetException(); 
            } else {
                cause = cause.getCause();
            }
        }
        return Optional.empty();

    }
}
