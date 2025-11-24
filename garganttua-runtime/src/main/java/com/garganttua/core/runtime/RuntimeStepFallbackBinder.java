package com.garganttua.core.runtime;

import static com.garganttua.core.runtime.RuntimeStepExecutionTools.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public class RuntimeStepFallbackBinder<ExecutionReturned, InputType, OutputType> implements
        IRuntimeStepFallbackBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {

    private final String runtimeName;
    private final String stageName;
    private final String stepName;
    private final IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate;
    private final Optional<String> variable;
    private final Boolean isOutput;
    private final List<IRuntimeStepOnException> onExceptions;
    private final Boolean nullable;

    public RuntimeStepFallbackBinder(String runtimeName, String stageName, String stepName,
            IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate,
            Optional<String> variable, Boolean isOutput, List<IRuntimeStepOnException> onExceptions, Boolean nullable) {
        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "stageName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
        this.onExceptions = List.copyOf(Objects.requireNonNull(onExceptions, "OnException list cannot be null"));
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
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
        return nullable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void fallBack(IRuntimeContext<InputType, OutputType> context,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> nextExecutor) {

        Optional<String> variable = variable();
        ExecutionReturned returned = null;
        Optional<RuntimeExceptionRecord> abortingException = context.findAbortingExceptionReport();

        if( abortingException.isEmpty() ){
            handleException(this.runtimeName, this.stageName, this.stepName, context, new ExecutorException(
                            logLineHeader()
                                    + "Fallback method is executed but no aborting exception found !"), true,
                    this.getExecutableReference(), null, logLineHeader());

            nextExecutor.executeFallBack(context);
            return;
        }

        if (this.findMatchingOnException(abortingException.get()).isEmpty()){
            nextExecutor.executeFallBack(context);
            return;
        }

        try {
            returned = execute(context).orElse(null);
        } catch (Exception e) {
            handleException(this.runtimeName, this.stageName, this.stepName, context, e, false,
                    this.getExecutableReference(), null, logLineHeader());
        }

        if (isOutput()) {
            validateReturnedForOutput(this.runtimeName, this.stageName, this.stepName, returned, context, nullable(),
                    logLineHeader(), getExecutableReference());
            context.setOutput((OutputType) returned);
        }

        if (variable.isPresent())
            validateAndStoreReturnedValueInVariable(this.runtimeName, this.stageName, this.stepName, variable.get(),
                    returned, context, nullable(), logLineHeader(), getExecutableReference());

        nextExecutor.executeFallBack(context);
    }

    private Optional<IRuntimeStepOnException> findMatchingOnException(RuntimeExceptionRecord abortingExceptionReport) {
        return this.onExceptions.stream().filter(o -> abortingExceptionReport.matches(o)).findFirst();
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "][Fallback "
                + this.delegate.getExecutableReference() + "] ";
    }
}
