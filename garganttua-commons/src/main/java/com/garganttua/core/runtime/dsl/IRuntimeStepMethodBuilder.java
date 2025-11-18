package com.garganttua.core.runtime.dsl;

import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepMethodBinder;

public interface IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
        IMethodBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>>>,
        IContextBuilderObserver {

    boolean isThrown(Class<? extends Throwable> exception);

    IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(String variableName);

    IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output);

    IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(Integer code);

}
