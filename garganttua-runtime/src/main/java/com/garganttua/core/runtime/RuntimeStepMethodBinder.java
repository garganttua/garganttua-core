package com.garganttua.core.runtime;

import static com.garganttua.core.runtime.RuntimeStepExecutionTools.*;

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

    private final Set<IRuntimeStepCatch> catches;
    private final IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate;
    private final Optional<String> variable;
    private final boolean isOutput;

    private final Integer code;
    private final String runtimeName;
    private final String stageName;
    private final String stepName;
    private final Optional<ICondition> condition;

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
        this.catches = Set.copyOf(Objects.requireNonNull(catches, "Catches cannot be null"));
        this.condition = Objects.requireNonNull(condition, "Condition optional cannot be null");
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

        if (!condition.map(ICondition::evaluate).orElse(true)){
            next.execute(context);
            return;
        }

        Optional<String> variable = variable();
        ExecutionReturned returned = null;

        try {
            returned = execute(context).orElse(null);
        } catch (Exception e) {
            IRuntimeStepCatch matchedCatch = findMatchingCatch(e);
            handleException(this.runtimeName, this.stageName, this.stepName, context, e, false,
                    this.getExecutableReference(), matchedCatch, logLineHeader());
        }

        if (isOutput()) {
            validateReturnedForOutput(this.runtimeName, this.stageName, this.stepName, returned, context, nullable(),
                    logLineHeader(), getExecutableReference());
            context.setOutput((OutputType) returned);
            setCode(context);
        }

        if (variable.isPresent())
            validateAndStoreReturnedValueInVariable(this.runtimeName, this.stageName, this.stepName, variable.get(),
                    returned, context, nullable(), logLineHeader(), getExecutableReference());

        next.execute(context);
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

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "][Method "
                + this.delegate.getExecutableReference() + "] ";
    }
}
