package com.garganttua.core.runtime;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

public record RuntimeStepOnException(IClass<? extends Throwable> exception, String runtimeName, String fromStep) implements IRuntimeStepOnException {
    private static final Logger log = Logger.getLogger(RuntimeStepOnException.class);

}
