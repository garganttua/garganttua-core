package com.garganttua.core.runtime;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record RuntimeStepCatch(Class<? extends Throwable> exception, Integer code) implements IRuntimeStepCatch {

}
