package com.garganttua.core.runtime.dsl;

import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;

public interface IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> extends
        IMethodBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType>, IMethodBinder<ExecutionReturn>>, IContextBuilderObserver {

    IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> variable(String variableName);

    IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> output(boolean output);
}
