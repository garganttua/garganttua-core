package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.CoreException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepMethodBinder<ExecutionReturned, InputType, OutputType>
        implements
        IRuntimeStepMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {

    private final Set<IRuntimeStepCatch> catches;
    private final IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression;
    private final Optional<String> variable;
    private final boolean isOutput;

    private final Integer code;
    private final String runtimeName;
    private final String stepName;
    private final Optional<ICondition> condition;
    private final Boolean abortOnUncatchedException;
    private final Boolean nullable;
    private final String expressionReference;

    public RuntimeStepMethodBinder(String runtimeName, String stepName,
            IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression,
            Optional<String> variable, boolean isOutput, Integer successCode, Set<IRuntimeStepCatch> catches,
            Optional<ICondition> condition, Boolean abortOnUncatchedException, Boolean nullable,
            String expressionReference) {

        log.atTrace().log(
                "[RuntimeStepMethodBinder.<init>] Initializing method binder: runtime={}, step={}, expression={}, variablePresent={}, isOutput={}, nullable={}",
                runtimeName, stepName, expressionReference, variable.isPresent(), isOutput, nullable);

        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.expression = Objects.requireNonNull(expression, "Expression cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
        this.code = Objects.requireNonNull(successCode, "Success code cannot be null");
        this.catches = Set.copyOf(Objects.requireNonNull(catches, "Catches cannot be null"));
        this.condition = Objects.requireNonNull(condition, "Condition optional cannot be null");
        this.abortOnUncatchedException = Objects.requireNonNull(abortOnUncatchedException,
                "abortOnUncatchedException cannot be null");
        this.nullable = Objects.requireNonNull(nullable, "nullable cannot be null");
        this.expressionReference = Objects.requireNonNull(expressionReference, "expressionReference cannot be null");

        log.atDebug().log("{}Method binder initialized. Catches count={}", logLineHeader(), this.catches.size());
    }

    @Override
    public Set<Class<?>> dependencies() {
        return Set.of();
    }

    @Override
    public Class<IRuntimeContext<InputType, OutputType>> getOwnerContextType() {
        return null;
    }

    @Override
    public Class<?>[] getParametersContextTypes() {
        return new Class<?>[0];
    }

    @Override
    public Optional<IMethodReturn<ExecutionReturned>> execute(IRuntimeContext<InputType, OutputType> ownerContext,
            Object... contexts) throws ReflectionException {
        log.atDebug().log("{}Evaluating expression via execute()", logLineHeader());
        RuntimeExpressionContext.set(ownerContext);
        try {
            ISupplier<ExecutionReturned> supplier = expression.evaluate();
            Optional<ExecutionReturned> result = supplier.supply();
            return result.map(r -> SingleMethodReturn.of(r));
        } catch (Exception e) {
            return Optional.of(SingleMethodReturn.ofException(e, null));
        } finally {
            RuntimeExpressionContext.clear();
        }
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
        return this.expressionReference;
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

        log.atDebug().log("{}Starting method execution", logLineHeader());

        if (!condition.map(ICondition::evaluate).orElse(new FixedSupplier<Boolean>(true)).supply().get()) {
            log.atTrace().log("{}Condition not met, skipping step", logLineHeader());
            next.execute(context);
            return;
        }

        Optional<String> variable = variable();
        ExecutionReturned returned = null;

        try {
            log.atDebug().log("{}Evaluating expression", logLineHeader());
            RuntimeExpressionContext.set(context);
            try {
                ISupplier<ExecutionReturned> supplier = expression.evaluate();
                Optional<ExecutionReturned> result = supplier.supply();
                returned = result.orElse(null);
            } finally {
                RuntimeExpressionContext.clear();
            }
            log.atTrace().log("{}Returned value={}", logLineHeader(), returned);
            processExecutionReturn(context, variable, returned);
        } catch (Exception e) {
            log.atWarn().log("{}Exception during expression evaluation: {}", logLineHeader(), e.getMessage(), e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            IRuntimeStepCatch matchedCatch = findMatchingCatch(cause);
            boolean forceAbort = matchedCatch == null && this.abortOnUncatchedException || matchedCatch != null;
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stepName, context, cause,
                    forceAbort, this.expressionReference, matchedCatch, logLineHeader());
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
            log.atDebug().log("{}Validating method output", logLineHeader());
            RuntimeStepExecutionTools.validateReturnedForOutput(this.runtimeName, this.stepName,
                    returned, context, nullable(), logLineHeader(), this.expressionReference);
            setCode(context);
        }

        if (variable.isPresent()) {
            log.atDebug().log("{}Storing returned value in variable '{}'", logLineHeader(), variable.get());
            RuntimeStepExecutionTools.validateAndStoreReturnedValueInVariable(this.runtimeName,
                    this.stepName, variable.get(), returned, context, nullable(), logLineHeader(),
                    this.expressionReference);
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
        return "[Runtime " + runtimeName + "][Step " + stepName + "][Expression "
                + this.expressionReference + "] ";
    }

    @Override
    public Type getSuppliedType() {
        return this.expression.getSuppliedClass();
    }

    @Override
    public Optional<IMethodReturn<ExecutionReturned>> supply(IRuntimeContext<InputType, OutputType> ownerContext,
            Object... otherContexts) throws SupplyException {
        return this.execute(ownerContext, otherContexts);
    }
}
