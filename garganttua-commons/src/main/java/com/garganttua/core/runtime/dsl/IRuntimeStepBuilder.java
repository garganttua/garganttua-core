package com.garganttua.core.runtime.dsl;

import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IRuntimeStep;

public interface IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> extends
                IAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStageBuilder<InputType, OutputType>, IRuntimeStep<?, InputType, OutputType>>,
                IContextBuilderObserver {

        IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> condition(
                        IConditionBuilder conditionBuilder);

        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> method() throws DslException;

        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallBack() throws DslException;

        IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> katch(Class<? extends Throwable> exception) throws DslException;

}
