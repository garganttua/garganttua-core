package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStepFallbackBinder<ExecutionReturned, InputType, OutputType> implements
        IRuntimeStepFallbackBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {

    private final String runtimeName;
    private final String stepName;
    private final IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression;
    private final Optional<String> variable;
    private final Boolean isOutput;
    private final List<IRuntimeStepOnException> onExceptions;
    private final Boolean nullable;
    private final String expressionReference;

    public RuntimeStepFallbackBinder(String runtimeName, String stepName,
            IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression,
            Optional<String> variable, Boolean isOutput, List<IRuntimeStepOnException> onExceptions, Boolean nullable,
            String expressionReference) {

        log.atTrace().log(
                "[RuntimeStepFallbackBinder.<init>] Initializing fallback: runtime={}, step={}, expression={}, variablePresent={}, isOutput={}, nullable={}",
                runtimeName, stepName, expressionReference, variable.isPresent(), isOutput, nullable);

        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.expression = Objects.requireNonNull(expression, "Expression cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
        this.onExceptions = List.copyOf(Objects.requireNonNull(onExceptions, "OnException list cannot be null"));
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        this.expressionReference = Objects.requireNonNull(expressionReference, "expressionReference cannot be null");

        log.atDebug().log("{}Fallback binder initialized. OnExceptions count={}", logLineHeader(),
                this.onExceptions.size());
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
        log.atDebug().log("{}Evaluating fallback expression via execute()", logLineHeader());
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
    public String getExecutableReference() {
        return this.expressionReference;
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

        log.atDebug().log("{}Executing fallback logic", logLineHeader());

        Optional<String> variable = variable();
        ExecutionReturned returned = null;
        Optional<RuntimeExceptionRecord> abortingException = context.findAbortingExceptionReport();

        if (abortingException.isEmpty()) {
            log.atWarn().log("{}Fallback executed but no aborting exception found!", logLineHeader());
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stepName, context,
                    new ExecutorException(
                            logLineHeader() + "Fallback method is executed but no aborting exception found!"),
                    true, this.expressionReference, null, logLineHeader());

            nextExecutor.executeFallBack(context);
            return;
        }

        if (this.findMatchingOnException(abortingException.get()).isEmpty()) {
            log.atTrace().log("{}No matching onException found, executing next fallback in chain", logLineHeader());
            nextExecutor.executeFallBack(context);
            return;
        }

        try {
            log.atDebug().log("{}Evaluating fallback expression", logLineHeader());
            RuntimeExpressionContext.set(context);
            try {
                ISupplier<ExecutionReturned> supplier = expression.evaluate();
                Optional<ExecutionReturned> result = supplier.supply();
                returned = result.orElse(null);
            } finally {
                RuntimeExpressionContext.clear();
            }
            log.atTrace().log("{}Fallback returned value={}", logLineHeader(), returned);
        } catch (Exception e) {
            log.atWarn().log("{}Exception occurred during fallback execution: {}", logLineHeader(), e.getMessage(), e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stepName, context, cause,
                    false,
                    this.expressionReference, null, logLineHeader());
        }

        if (isOutput()) {
            log.atDebug().log("{}Validating fallback as output", logLineHeader());
            RuntimeStepExecutionTools.validateReturnedForOutput(this.runtimeName, this.stepName,
                    returned,
                    context, nullable(), logLineHeader(), this.expressionReference);
            context.setOutput((OutputType) returned);
        }

        if (variable.isPresent()) {
            log.atDebug().log("{}Storing fallback returned value in variable '{}'", logLineHeader(), variable.get());
            RuntimeStepExecutionTools.validateAndStoreReturnedValueInVariable(this.runtimeName,
                    this.stepName, variable.get(), returned, context, nullable(), logLineHeader(),
                    this.expressionReference);
        }

        log.atDebug().log("{}Executing next fallback in chain", logLineHeader());
        nextExecutor.executeFallBack(context);
    }

    private Optional<IRuntimeStepOnException> findMatchingOnException(RuntimeExceptionRecord abortingExceptionReport) {
        return this.onExceptions.stream().filter(o -> abortingExceptionReport.matches(o)).findFirst();
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "][Fallback "
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
