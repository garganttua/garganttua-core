package com.garganttua.core.runtime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RuntimeStepOnException(Class<? extends Throwable> exception, String runtimeName, String fromStep) implements IRuntimeStepOnException {

}
