package com.garganttua.core.runtime;

public interface IRuntimeStepOnException {

    String runtimeName();

    String fromStage();

    String fromStep();

    Class<? extends Throwable> exception();

}
