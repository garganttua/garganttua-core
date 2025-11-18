package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStepCatch;

public interface IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>
        extends IAutomaticLinkedBuilder<IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType>, IRuntimeStepCatch> {

    IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> code(int i);

    IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> fallback(boolean fallback);

    IRuntimeStepCatchBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> abort(boolean abord);

}
