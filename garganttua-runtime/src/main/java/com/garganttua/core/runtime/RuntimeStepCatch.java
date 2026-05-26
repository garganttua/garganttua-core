package com.garganttua.core.runtime;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;

public record RuntimeStepCatch(IClass<? extends Throwable> exception, Integer code) implements IRuntimeStepCatch {
    private static final IDiagnostic log = Diagnostics.of(RuntimeStepCatch.class);

}
