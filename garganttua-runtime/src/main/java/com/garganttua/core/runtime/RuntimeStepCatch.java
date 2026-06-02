package com.garganttua.core.runtime;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

public record RuntimeStepCatch(IClass<? extends Throwable> exception, Integer code) implements IRuntimeStepCatch {
    private static final Logger log = Logger.getLogger(RuntimeStepCatch.class);

}
