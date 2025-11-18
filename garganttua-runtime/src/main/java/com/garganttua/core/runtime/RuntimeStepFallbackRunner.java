package com.garganttua.core.runtime;

import java.util.Objects;

import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.execution.IFallBackExecutor;

public class RuntimeStepFallbackRunner<InputType, OutputType> implements IFallBackExecutor<IRuntimeContext<InputType, OutputType>>{


    private IRuntimeStep step;

    public RuntimeStepFallbackRunner(IRuntimeStep step) {
        this.step = Objects.requireNonNull(step, "Step cannot be null");
    }

    @Override
    public void fallBack(IRuntimeContext<InputType, OutputType> request,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> nextExecutor) {
       



                nextExecutor.executeFallBack(request);
    }

}
