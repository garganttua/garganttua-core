package com.garganttua.core.runtime;

public record RuntimeStepOnException(Class<? extends Throwable> exception, String runtimeName, String fromStage, String fromStep) implements IRuntimeStepOnException {

}
