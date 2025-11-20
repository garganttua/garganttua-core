package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.execution.IExecutorChain;

public class RuntimeStep<ExecutionReturn, InputType, OutputType>
        implements IRuntimeStep<ExecutionReturn, InputType, OutputType> {

    private final String stepName;
    private Class<ExecutionReturn> executionReturn;
    private IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> operationBinder;
    private Optional<IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>> fallbackBinder;
    private String stageName;
    private String runtimeName;

    public RuntimeStep(String runtimeName, String stageName, String stepName, Class<ExecutionReturn> executionReturn,
            IRuntimeStepMethodBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType> operationBinder,
            Optional<IRuntimeStepFallbackBinder<ExecutionReturn, IRuntimeContext<InputType, OutputType>, InputType, OutputType>> fallbackBinder) {
        this.runtimeName = runtimeName;
        this.stageName = stageName;
        this.stepName = stepName;
        this.executionReturn = executionReturn;
        this.operationBinder = operationBinder;
        this.fallbackBinder = fallbackBinder;
    }

    @Override
    public String getStepName() {
        return stepName;
    }

    @Override
    public void defineExecutionStep(IExecutorChain<IRuntimeContext<InputType, OutputType>> chain) {
        if (this.fallbackBinder.isPresent())
            chain.addExecutor(operationBinder, fallbackBinder.get());
        else
            chain.addExecutor(operationBinder);
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "] ";
    }

}
