package com.garganttua.core.runtime;

import com.garganttua.core.runtime.dsl.IRuntimeStepCatch;

public record RuntimeStepCatch(Class<? extends Throwable> exception, Integer code, Boolean fallback, Boolean abort) implements IRuntimeStepCatch {

}
