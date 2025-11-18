package com.garganttua.core.runtime;

import com.garganttua.core.execution.IExecutorChain;

public interface IRuntimeStep<ExecutionReturn, InputType, OutputType> {

    String getStepName();

    void defineExecutionStep(IExecutorChain<IRuntimeContext<InputType,OutputType>> chain);

}
