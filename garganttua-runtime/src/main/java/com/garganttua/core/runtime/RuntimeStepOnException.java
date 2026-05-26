package com.garganttua.core.runtime;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;

public record RuntimeStepOnException(IClass<? extends Throwable> exception, String runtimeName, String fromStep) implements IRuntimeStepOnException {
    private static final IDiagnostic log = Diagnostics.of(RuntimeStepOnException.class);

}
