package com.garganttua.core.runtime;

import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RuntimeStepOnException(IClass<? extends Throwable> exception, String runtimeName, String fromStep) implements IRuntimeStepOnException {

}
