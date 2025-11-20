package com.garganttua.core.runtime;

public interface IRuntimeStepCatch {

    Class<? extends Throwable> exception();

    Integer code();

}
