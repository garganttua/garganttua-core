package com.garganttua.core.runtime;

import com.garganttua.core.reflection.binders.IMethodBinder;

public interface IRuntimeStep<ExecutionReturn> extends IMethodBinder<ExecutionReturn> {

    String getStepName();

}
