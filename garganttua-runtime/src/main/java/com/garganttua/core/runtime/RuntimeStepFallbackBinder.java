package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Binds a step's fallback expression: when a step aborts with an exception that
 * matches one of this binder's {@code onException} declarations, the fallback
 * expression is evaluated and its result is stored as a variable and/or the
 * runtime output, before delegating to the next fallback in the chain.
 *
 * @param <ExecutionReturned> the fallback expression's return type
 * @param <InputType>         the runtime input type
 * @param <OutputType>        the runtime output type
 */
public class RuntimeStepFallbackBinder<ExecutionReturned, InputType, OutputType> implements
        IRuntimeStepFallbackBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepFallbackBinder.class);

    private final String runtimeName;
    private final String stepName;
    private final IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression;
    private final Optional<String> variable;
    private final Boolean isOutput;
    private final List<IRuntimeStepOnException> onExceptions;
    private final Boolean nullable;
    private final String expressionReference;

    /**
     * Creates a fallback binder.
     *
     * @param runtimeName         the owning runtime name
     * @param stepName            the step name
     * @param expression          the fallback expression to evaluate
     * @param variable            the optional variable to store the fallback result in
     * @param isOutput            whether the fallback result becomes the runtime output
     * @param onExceptions        the exception declarations this fallback handles
     * @param nullable            whether a {@code null} fallback result is permitted
     * @param expressionReference reference to the expression, for diagnostics
     */
    public RuntimeStepFallbackBinder(String runtimeName, String stepName,
            IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression,
            Optional<String> variable, Boolean isOutput, List<IRuntimeStepOnException> onExceptions, Boolean nullable,
            String expressionReference) {

        log.trace(
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

        log.debug("{}Fallback binder initialized. OnExceptions count={}", logLineHeader(),
                this.onExceptions.size());
    }

    @Override
    public Set<IClass<?>> dependencies() {
        return Set.of();
    }

    @Override
    public IClass<IRuntimeContext<InputType, OutputType>> getOwnerContextType() {
        return null;
    }

    @Override
    public IClass<?>[] getParametersContextTypes() {
        return new IClass<?>[0];
    }

    @Override
    public Optional<IMethodReturn<ExecutionReturned>> execute(IRuntimeContext<InputType, OutputType> ownerContext,
            Object... contexts) throws ReflectionException {
        log.debug("{}Evaluating fallback expression via execute()", logLineHeader());
        return RuntimeExpressionContext.callIn(ownerContext, () -> {
            try {
                ISupplier<ExecutionReturned> supplier = expression.evaluate();
                Optional<ExecutionReturned> result = supplier.supply();
                return result.map(r -> SingleMethodReturn.of(r, expression.getSuppliedClass()));
            } catch (Exception e) {
                return Optional.of(SingleMethodReturn.ofException(e, null));
            }
        });
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

        log.debug("{}Executing fallback logic", logLineHeader());

        Optional<String> variable = variable();
        ExecutionReturned returned = null;
        Optional<RuntimeExceptionRecord> abortingException = context.findAbortingExceptionReport();

        if (abortingException.isEmpty()) {
            log.warn("{}Fallback executed but no aborting exception found!", logLineHeader());
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stepName, context,
                    new ExecutorException(
                            logLineHeader() + "Fallback method is executed but no aborting exception found!"),
                    true, this.expressionReference, null, logLineHeader());

            nextExecutor.executeFallBack(context);
            return;
        }

        if (this.findMatchingOnException(abortingException.get()).isEmpty()) {
            log.trace("{}No matching onException found, executing next fallback in chain", logLineHeader());
            nextExecutor.executeFallBack(context);
            return;
        }

        try {
            log.debug("{}Evaluating fallback expression", logLineHeader());
            returned = RuntimeExpressionContext.callIn(context, () -> {
                ISupplier<ExecutionReturned> supplier = expression.evaluate();
                Optional<ExecutionReturned> result = supplier.supply();
                return result.orElse(null);
            });
            log.trace("{}Fallback returned value={}", logLineHeader(), returned);
        } catch (Exception e) {
            log.warn("{}Exception occurred during fallback execution: {}", logLineHeader(), e.getMessage(), e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stepName, context, cause,
                    false,
                    this.expressionReference, null, logLineHeader());
        }

        if (isOutput()) {
            log.debug("{}Validating fallback as output", logLineHeader());
            RuntimeStepExecutionTools.validateReturnedForOutput(this.runtimeName, this.stepName,
                    returned,
                    context, nullable(), logLineHeader(), this.expressionReference);
            context.setOutput((OutputType) returned);
        }

        if (variable.isPresent()) {
            log.debug("{}Storing fallback returned value in variable '{}'", logLineHeader(), variable.get());
            RuntimeStepExecutionTools.validateAndStoreReturnedValueInVariable(this.runtimeName,
                    this.stepName, variable.get(), returned, context, nullable(), logLineHeader(),
                    this.expressionReference);
        }

        log.debug("{}Executing next fallback in chain", logLineHeader());
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
        return this.expression.getSuppliedType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public IClass<IMethodReturn<ExecutionReturned>> getSuppliedClass() {
        return (IClass<IMethodReturn<ExecutionReturned>>) (IClass<?>) IClass.getClass(IMethodReturn.class);
    }

    @Override
    public Optional<IMethodReturn<ExecutionReturned>> supply(IRuntimeContext<InputType, OutputType> ownerContext,
            Object... otherContexts) throws SupplyException {
        return this.execute(ownerContext, otherContexts);
    }
}
