package com.garganttua.core.runtime;

public record RuntimeStepOnException(Class<? extends Throwable> exception, String stageName, String stepName) implements IRuntimeStepOnException {

}
