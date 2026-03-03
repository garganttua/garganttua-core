package com.garganttua.core.runtime;

import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RuntimeStepCatch(IClass<? extends Throwable> exception, Integer code) implements IRuntimeStepCatch {

}
