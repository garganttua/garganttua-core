package com.garganttua.core.runtime.dsl;

import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IRuntimeStep;

public interface IRuntimeStepBuilder<ExecutionReturn, StepObjectType> extends IAutomaticLinkedBuilder<IRuntimeStepBuilder<ExecutionReturn, StepObjectType>, IRuntimeStageBuilder<?,?>, IRuntimeStep>, IContextBuilderObserver {

        IRuntimeStepBuilder<ExecutionReturn, StepObjectType> condition(IConditionBuilder conditionBuilder);

        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> method() throws DslException;

        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> fallBack() throws DslException;

        IRuntimeStepCatchBuilder katch(Class<? extends Throwable> exception) throws DslException;

}
