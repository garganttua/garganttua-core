package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStepCatch;

public interface IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends IAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch> {

    IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i);

}
