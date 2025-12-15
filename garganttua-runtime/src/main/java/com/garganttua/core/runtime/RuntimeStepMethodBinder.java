package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.CoreException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private final Boolean abortOnUncatchedException;
    private final Boolean nullable;

    public RuntimeStepMethodBinder(String runtimeName, String stageName, String stepName,
            IContextualMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>> delegate,
            Optional<String> variable, boolean isOutput, Integer successCode, Set<IRuntimeStepCatch> catches,
            Optional<ICondition> condition, Boolean abortOnUncatchedException, Boolean nullable) {

        log.atTrace().log(
                "[RuntimeStepMethodBinder.<init>] Initializing method binder: runtime={}, stage={}, step={}, delegate={}, variablePresent={}, isOutput={}, nullable={}",
                runtimeName, stageName, stepName, delegate, variable.isPresent(), isOutput, nullable);

        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stageName = Objects.requireNonNull(stageName, "stageName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.delegate = Objects.requireNonNull(delegate, "Delegate cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
        this.code = Objects.requireNonNull(successCode, "Success code cannot be null");
        this.catches = Set.copyOf(Objects.requireNonNull(catches, "Catches cannot be null"));
        this.condition = Objects.requireNonNull(condition, "Condition optional cannot be null");
        this.abortOnUncatchedException = Objects.requireNonNull(abortOnUncatchedException,
                "abortOnUncatchedException cannot be null");
        this.nullable = Objects.requireNonNull(nullable, "nullable cannot be null");

        log.atInfo().log("{}Method binder initialized. Catches count={}", logLineHeader(), this.catches.size());
    }

    @Override
    public Set<Class<?>> dependencies() {
        return this.delegate.dependencies();
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
        log.atDebug().log("{}Executing method delegate", logLineHeader());
        return this.delegate.execute(ownerContext, contexts);
    }

    @Override
    public boolean isOutput() {
        return this.isOutput;
    }

    @Override
    public void setCode(IRuntimeContext<?, ?> c) {
        log.atTrace().log("{}Setting code {} on context", logLineHeader(), code);
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
        return this.nullable;
    }

    public void execute(IRuntimeContext<InputType, OutputType> context,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> next) throws ExecutorException {

        log.atInfo().log("{}Starting method execution", logLineHeader());

        if (!condition.map(ICondition::evaluate).orElse(new FixedSupplier<Boolean>(true)).supply().get()) {
            log.atTrace().log("{}Condition not met, skipping step", logLineHeader());
            next.execute(context);
            return;
        }

        Optional<String> variable = variable();
        ExecutionReturned returned = null;

        try {
            log.atDebug().log("{}Invoking method", logLineHeader());
            returned = execute(context).orElse(null);
            log.atTrace().log("{}Returned value={}", logLineHeader(), returned);
            processExecutionReturn(context, variable, returned);
        } catch (Exception e) {
            log.atWarn().log("{}Exception during method execution: {}", logLineHeader(), e.getMessage(), e);
            IRuntimeStepCatch matchedCatch = findMatchingCatch(e);
            boolean forceAbort = matchedCatch == null && this.abortOnUncatchedException || matchedCatch != null;
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stageName, this.stepName, context, e,
                    forceAbort, this.getExecutableReference(), matchedCatch, logLineHeader());
            if (!forceAbort) {
                log.atDebug().log("{}Processing return despite exception (non-aborting)", logLineHeader());
                processExecutionReturn(context, variable, returned);
            }
        }

        log.atTrace().log("{}Executing next in chain", logLineHeader());
        next.execute(context);
    }

    private void processExecutionReturn(IRuntimeContext<InputType, OutputType> context, Optional<String> variable,
            ExecutionReturned returned) {

        if (isOutput()) {
            log.atInfo().log("{}Validating method output", logLineHeader());
            RuntimeStepExecutionTools.validateReturnedForOutput(this.runtimeName, this.stageName, this.stepName,
                    returned, context, nullable(), logLineHeader(), getExecutableReference());
            setCode(context);
        }

        if (variable.isPresent()) {
            log.atInfo().log("{}Storing returned value in variable '{}'", logLineHeader(), variable.get());
            RuntimeStepExecutionTools.validateAndStoreReturnedValueInVariable(this.runtimeName, this.stageName,
                    this.stepName, variable.get(), returned, context, nullable(), logLineHeader(),
                    getExecutableReference());
        }
    }

    private IRuntimeStepCatch findMatchingCatch(Throwable exception) {
        for (IRuntimeStepCatch stepCatch : this.catches) {
            Optional<? extends Throwable> cause = CoreException.findFirstInException(exception, stepCatch.exception());
            if (cause.isPresent()) {
                log.atDebug().log("{}Matching catch found for exception: {}", logLineHeader(),
                        cause.get().getClass().getSimpleName());
                return stepCatch;
            }
        }
        log.atTrace().log("{}No matching catch found", logLineHeader());
        return null;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Stage " + stageName + "][Step " + stepName + "][Method "
                + this.delegate.getExecutableReference() + "] ";
    }

    @Override
    public Type getSuppliedType() {
        return this.delegate.getSuppliedClass();
    }

    @Override
    public Optional<ExecutionReturned> supply(IRuntimeContext<InputType, OutputType> ownerContext,
            Object... otherContexts) throws SupplyException {
        return this.execute(ownerContext, otherContexts);
    }
}
