package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepFallbackBinder;

public interface IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                IMethodBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>>,
                IContextBuilderObserver {

        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> variable(
                        String variableName);

        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> output(boolean output);

        IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> onException(
                        Class<? extends Throwable> exception) throws DslException;

        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> nullable(
                        boolean nullable);

}
