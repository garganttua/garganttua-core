package com.garganttua.core.runtime;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.CoreException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public class RuntimeStepMethodBinder<ExecutionReturned, InputType, OutputType>
        implements
        IRuntimeStepMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {

    private Set<IRuntimeStepCatch> catches;
    private IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate;
    private Optional<String> variable;
    private boolean isOutput;

    private Integer code;
    private String runtimeName;
    private String stageName;
    private String stepName;
    private Optional<ICondition> condition;

    public RuntimeStepMethodBinder(String runtimeName, String stageName, String stepName,
            IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate,
            Optional<String> variable, boolean isOutput, Integer successCode, Set<IRuntimeStepCatch> catches,
            Optional<ICondition> condition) {
        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "stageName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
        this.code = Objects.requireNonNull(successCode, "Success code cannot be null");
        this.catches = Objects.requireNonNull(catches, "Catches cannot be null");
        this.condition = Objects.requireNonNull(condition, "Condition optional cannot be null");
        ;
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
    public void setCode(IRuntimeContext<?, ?> c) {
        c.setCode(code);
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

    @SuppressWarnings("unchecked")
    public void execute(IRuntimeContext<InputType, OutputType> context,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> next) throws ExecutorException {

        if (!condition.map(ICondition::evaluate).orElse(true))
            next.execute(context);

        Optional<String> variable = variable();
        ExecutionReturned returned = null;

        try {
            returned = execute(context).orElse(null);
        } catch (Exception e) {
            handleException(context, e);
        }

        if (isOutput()) {
            validateReturnedForOutput(returned, context);
            context.setOutput((OutputType) returned);
            setCode(context);
        }

        if (variable.isPresent())
            validateAndStoreVariable(variable.get(), returned, context);

        next.execute(context);
    }

    private void validateReturnedForOutput(
            ExecutionReturned returned,
            IRuntimeContext<InputType, OutputType> context) throws ExecutorException {

        if (returned == null && !nullable()) {
            handleException(
                    context,
                    new ExecutorException(
                            logLineHeader()
                                    + "is defined to be output but did not return any value and is not nullable"),
                    true);
            return;
        }

        if (returned != null && !context.isOfOutputType(returned.getClass())) {
            handleException(
                    context,
                    new ExecutorException(
                            logLineHeader()
                                    + "is defined to be output, but returned type "
                                    + returned.getClass().getSimpleName()
                                    + " is not output type "
                                    + context.getOutputType().getSimpleName()),
                    true);
        }
    }

    private void validateAndStoreVariable(
            String variableName,
            ExecutionReturned returned,
            IRuntimeContext<InputType, OutputType> context) throws ExecutorException {

        if (returned == null && !nullable()) {
            handleException(
                    context,
                    new ExecutorException(
                            logLineHeader()
                                    + "is defined to store return in variable "
                                    + variableName
                                    + " but did not return any value and is not nullable"),
                    true);
            return;
        }

        context.setVariable(variableName, returned);
    }

    private void handleException(IRuntimeContext<InputType, OutputType> context, Exception exception)
            throws ExecutorException {
        this.handleException(context, exception, false);
    }

    private IRuntimeStepCatch findMatchingCatch(Throwable exception) {
        for (IRuntimeStepCatch stepCatch : this.catches) {
            Optional<? extends Throwable> cause = CoreException.findFirstInException(exception, stepCatch.exception());
            if (cause.isPresent()) {
                return stepCatch;
            }
        }
        return null;
    }

    private void handleException(
            IRuntimeContext<InputType, OutputType> context,
            Throwable exception,
            boolean forceAbort) throws ExecutorException {

        Throwable reportException = exception;
        int reportCode = -1;
        boolean aborted = forceAbort;

        try {
            if (forceAbort) {
                reportCode = IRuntime.GENERIC_RUNTIME_ERROR_CODE;
                aborted = true;
                throw new ExecutorException(logLineHeader() + "Error during step execution", exception);
            }

            IRuntimeStepCatch matchedCatch = findMatchingCatch(exception);

            if (matchedCatch != null) {
                reportException = findExceptionForReport(exception, matchedCatch);
                reportCode = matchedCatch.code();
                aborted = true;

                throw new ExecutorException(logLineHeader() + "Error during step execution", exception);
            }

        } finally {
            context.recordException(new RuntimeExceptionRecord(
                    runtimeName,
                    stageName,
                    stepName,
                    reportException,
                    reportCode,
                    aborted));
            if (aborted) {
                context.setCode(reportCode);
            }
        }
    }

    private Throwable findExceptionForReport(Throwable exception, IRuntimeStepCatch matchedCatch) {
        Throwable reportException;
        Optional<? extends Throwable> found = CoreException
                .findFirstInException(exception, matchedCatch.exception());
        if (found.isPresent()) {
            reportException = found.get();
        } else {
            reportException = exception;
        }
        return reportException;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "][Method "
                + this.delegate.getExecutableReference() + "] ";
    }
}
