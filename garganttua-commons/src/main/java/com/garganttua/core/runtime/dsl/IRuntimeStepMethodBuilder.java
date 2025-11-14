package com.garganttua.core.runtime.dsl;

import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;

public interface IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> extends IMethodBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType>, IMethodBinder<ExecutionReturn>>{

    boolean isThrown(Class<? extends Throwable> exception);

}
