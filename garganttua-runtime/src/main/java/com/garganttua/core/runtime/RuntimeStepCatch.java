package com.garganttua.core.runtime;

public record RuntimeStepCatch(Class<? extends Throwable> exception, Integer code, Boolean fallback, Boolean abort) implements IRuntimeStepCatch {

}
