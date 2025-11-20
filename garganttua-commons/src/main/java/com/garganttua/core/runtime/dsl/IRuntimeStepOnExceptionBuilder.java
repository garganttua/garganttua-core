package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStepOnException;

public interface IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends
        IAutomaticLinkedBuilder<IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepOnException> {

    IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStage(String stageName);

    IRuntimeStepOnExceptionBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fromStep(String stepName);
}
