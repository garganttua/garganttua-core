package com.garganttua.core.runtime;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.CoreException;

public class RuntimeException extends CoreException {

    private Optional<IRuntimeContext<?, ?>> context;

    public RuntimeException(String message) {
        super(CoreException.RUNTIME_ERROR, message);
    }

    public RuntimeException(Exception e) {
        super(CoreException.RUNTIME_ERROR, e);
    }

    public RuntimeException(String string, Throwable e) {
        super(CoreException.RUNTIME_ERROR, string, e);
    }

    public RuntimeException(Exception e, Optional<IRuntimeContext<?,?>> context) {
        this(e);
        this.context = Objects.requireNonNull(context, "Context cannot be null");
    }

}
