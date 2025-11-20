package com.garganttua.core.runtime;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public class RuntimeStepFallbackBinder<ExecutionReturned, InputType, OutputType> implements IRuntimeStepFallbackBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {

    private String runtimeName;
    private String stageName;
    private String stepName;
    private IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate;
    private Optional<String> variable;
    private Boolean isOutput;

    public RuntimeStepFallbackBinder(String runtimeName, String stageName, String stepName,
            IContextualMethodBinder<ExecutionReturned,IRuntimeContext<InputType,OutputType>> delegate,
            Optional<String> variable, Boolean isOutput, List<IRuntimeStepOnException> onExceptions) {
        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "stageName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return this.delegate.getDependencies();
    }

    @Override
    public Class<IRuntimeContext<InputType, OutputType>> getOwnerContextType() {
        return this.delegate.getOwnerContextType();
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        return this.delegate.getParametersContextTypes();
    }

    @Override
    public Optional<ExecutionReturned> execute(IRuntimeContext<InputType, OutputType> ownerContext, Object... contexts)
            throws ReflectionException {
        return this.delegate.execute(ownerContext, contexts);
    }

    @Override
    public boolean isOutput() {
        return this.isOutput;
    }

    @Override
    public String getExecutableReference() {
        return this.delegate.getExecutableReference();
    }

    @Override
    public Optional<String> variable() {
        return this.variable;
    }

    @Override
    public boolean nullable() {
        return false;
    }

    @Override
    public void fallBack(IRuntimeContext<InputType, OutputType> request,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> nextExecutor) {
        nextExecutor.executeFallBack(request);
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "][Fallback "
                + this.delegate.getExecutableReference() + "] ";
    }
}
