package com.garganttua.core.runtime;

import java.util.Objects;

import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutor;
import com.garganttua.core.execution.IExecutorChain;

public class RuntimeStepOperationRunner<InputType, OutputType>
        implements IExecutor<IRuntimeContext<InputType, OutputType>> {

    private IRuntimeStep step;

    public RuntimeStepOperationRunner(IRuntimeStep step) {
        this.step = Objects.requireNonNull(step, "Step cannot be null");
    }

    @Override
    public void execute(IRuntimeContext<InputType, OutputType> request,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> nextExecutor) throws ExecutorException {
                


                nextExecutor.execute(request);
    }

}
