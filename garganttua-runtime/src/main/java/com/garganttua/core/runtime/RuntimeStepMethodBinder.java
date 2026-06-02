package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.CoreException;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.execution.ExecutorException;
import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.methods.SingleMethodReturn;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Binds a step's main operation expression and drives its execution: optional
 * condition gating, expression evaluation, pipe post-processing, catch/abort
 * handling, and storing the result as a variable and/or the runtime output.
 *
 * @param <ExecutionReturned> the expression's return type
 * @param <InputType>         the runtime input type
 * @param <OutputType>        the runtime output type
 */
public class RuntimeStepMethodBinder<ExecutionReturned, InputType, OutputType>
        implements
        IRuntimeStepMethodBinder<ExecutionReturned, IRuntimeContext<InputType, OutputType>, InputType, OutputType> {
    private static final Logger log = Logger.getLogger(RuntimeStepMethodBinder.class);

    private final Set<IRuntimeStepCatch> catches;
    private final List<IRuntimeStepPipe> pipes;
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

    /**
     * Creates a method binder with no pipes.
     *
     * @param runtimeName              the owning runtime name
     * @param stepName                 the step name
     * @param expression               the operation expression to evaluate
     * @param variable                 the optional variable to store the result in
     * @param isOutput                 whether the result becomes the runtime output
     * @param successCode              the exit code set on successful execution
     * @param catches                  the catch clauses for this step
     * @param condition                an optional condition gating execution
     * @param abortOnUncatchedException whether an uncaught exception aborts the runtime
     * @param nullable                 whether a {@code null} result is permitted
     * @param expressionReference      reference to the expression, for diagnostics
     */
    public RuntimeStepMethodBinder(String runtimeName, String stepName,
            IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression,
            Optional<String> variable, boolean isOutput, Integer successCode, Set<IRuntimeStepCatch> catches,
            Optional<ICondition> condition, Boolean abortOnUncatchedException, Boolean nullable,
            String expressionReference) {
        this(runtimeName, stepName, expression, variable, isOutput, successCode, catches, List.of(),
                condition, abortOnUncatchedException, nullable, expressionReference);
    }

    /**
     * Creates a method binder.
     *
     * @param runtimeName              the owning runtime name
     * @param stepName                 the step name
     * @param expression               the operation expression to evaluate
     * @param variable                 the optional variable to store the result in
     * @param isOutput                 whether the result becomes the runtime output
     * @param successCode              the exit code set on successful execution
     * @param catches                  the catch clauses for this step
     * @param pipes                    conditional pipes applied to the result
     * @param condition                an optional condition gating execution
     * @param abortOnUncatchedException whether an uncaught exception aborts the runtime
     * @param nullable                 whether a {@code null} result is permitted
     * @param expressionReference      reference to the expression, for diagnostics
     */
    public RuntimeStepMethodBinder(String runtimeName, String stepName,
            IExpression<ExecutionReturned, ? extends ISupplier<ExecutionReturned>> expression,
            Optional<String> variable, boolean isOutput, Integer successCode, Set<IRuntimeStepCatch> catches,
            List<IRuntimeStepPipe> pipes,
            Optional<ICondition> condition, Boolean abortOnUncatchedException, Boolean nullable,
            String expressionReference) {

        log.trace(
                "[RuntimeStepMethodBinder.<init>] Initializing method binder: runtime={}, step={}, expression={}, variablePresent={}, isOutput={}, nullable={}",
                runtimeName, stepName, expressionReference, variable.isPresent(), isOutput, nullable);

        this.runtimeName = Objects.requireNonNull(runtimeName, "runtimeName cannot be null");
        this.stepName = Objects.requireNonNull(stepName, "stepName cannot be null");
        this.expression = Objects.requireNonNull(expression, "Expression cannot be null");
        this.variable = Objects.requireNonNull(variable, "Variable optional cannot be null");
        this.isOutput = Objects.requireNonNull(isOutput, "Is output cannot be null");
        this.code = Objects.requireNonNull(successCode, "Success code cannot be null");
        this.catches = Set.copyOf(Objects.requireNonNull(catches, "Catches cannot be null"));
        this.pipes = List.copyOf(Objects.requireNonNull(pipes, "Pipes cannot be null"));
        this.condition = Objects.requireNonNull(condition, "Condition optional cannot be null");
        this.abortOnUncatchedException = Objects.requireNonNull(abortOnUncatchedException,
                "abortOnUncatchedException cannot be null");
        this.nullable = Objects.requireNonNull(nullable, "nullable cannot be null");
        this.expressionReference = Objects.requireNonNull(expressionReference, "expressionReference cannot be null");

        log.debug("{}Method binder initialized. Catches count={}, pipes count={}",
                logLineHeader(), this.catches.size(), this.pipes.size());
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
        log.debug("{}Evaluating expression via execute()", logLineHeader());
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
    public void setCode(IRuntimeContext<?, ?> c) {
        log.trace("{}Setting code {} on context", logLineHeader(), code);
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

    /**
     * Executes the step within the chain: evaluates the condition (skipping the
     * step when unmet), runs the expression and its pipes, handles catch/abort
     * outcomes, processes the result, and advances the chain.
     *
     * @param context the runtime context
     * @param next    the next executor in the chain
     * @throws ExecutorException if the step aborts with an unhandled exception
     */
    public void execute(IRuntimeContext<InputType, OutputType> context,
            IExecutorChain<IRuntimeContext<InputType, OutputType>> next) throws ExecutorException {

        log.debug("{}Starting method execution", logLineHeader());

        if (!condition.map(ICondition::evaluate).orElse(new FixedSupplier<Boolean>(true, IClass.getClass(Boolean.class))).supply().get()) {
            log.trace("{}Condition not met, skipping step", logLineHeader());
            next.execute(context);
            return;
        }

        Optional<String> variable = variable();
        ExecutionReturned returned = null;

        try {
            log.debug("{}Evaluating expression", logLineHeader());
            returned = RuntimeExpressionContext.callIn(context, () -> {
                ISupplier<ExecutionReturned> supplier = expression.evaluate();
                Optional<ExecutionReturned> result = supplier.supply();
                ExecutionReturned r = result.orElse(null);
                log.trace("{}Returned value={}", logLineHeader(), r);
                return evaluatePipes(context, r);
            });
            processExecutionReturn(context, variable, returned);
        } catch (CatchAwareExpression.CatchResultException cre) {
            // Catch handler matched and executed — extract result, stop chain
            log.debug("{}Catch handler matched, stopping chain", logLineHeader());
            returned = (ExecutionReturned) cre.getResult();
            // Use the handler's variable name if present, otherwise fall back to step's variable
            Optional<String> catchVar = cre.getVariableName() != null
                    ? Optional.of(cre.getVariableName()) : variable;
            processExecutionReturn(context, catchVar, returned);
            return; // Don't call next.execute() — chain stops
        } catch (Exception e) {
            log.warn("{}Exception during expression evaluation: {}", logLineHeader(), e.getMessage(), e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            IRuntimeStepCatch matchedCatch = findMatchingCatch(cause);
            boolean forceAbort = matchedCatch == null && this.abortOnUncatchedException || matchedCatch != null;
            RuntimeStepExecutionTools.handleException(this.runtimeName, this.stepName, context, cause,
                    forceAbort, this.expressionReference, matchedCatch, logLineHeader());
            if (!forceAbort) {
                log.debug("{}Processing return despite exception (non-aborting)", logLineHeader());
                processExecutionReturn(context, variable, returned);
            }
        }

        log.trace("{}Executing next in chain", logLineHeader());
        next.execute(context);
    }

    private void processExecutionReturn(IRuntimeContext<InputType, OutputType> context, Optional<String> variable,
            ExecutionReturned returned) {

        if (isOutput()) {
            log.debug("{}Validating method output", logLineHeader());
            RuntimeStepExecutionTools.validateReturnedForOutput(this.runtimeName, this.stepName,
                    returned, context, nullable(), logLineHeader(), this.expressionReference);
        }

        if (variable.isPresent()) {
            log.debug("{}Storing returned value in variable '{}'", logLineHeader(), variable.get());
            RuntimeStepExecutionTools.validateAndStoreReturnedValueInVariable(this.runtimeName,
                    this.stepName, variable.get(), returned, context, nullable(), logLineHeader(),
                    this.expressionReference);
        }

        if (code != 0) {
            setCode(context);
        }
    }

    @SuppressWarnings("unchecked")
    private ExecutionReturned evaluatePipes(IRuntimeContext<InputType, OutputType> context,
            ExecutionReturned currentResult) {
        if (pipes.isEmpty()) {
            return currentResult;
        }
        for (IRuntimeStepPipe pipe : pipes) {
            boolean matches;
            if (pipe.condition().isEmpty()) {
                // Default pipe — always matches
                matches = true;
            } else {
                try {
                    Object condResult = pipe.condition().get().evaluate().supply().orElse(null);
                    matches = condResult instanceof Boolean b && b;
                } catch (Exception e) {
                    log.warn("{}Pipe condition evaluation failed: {}", logLineHeader(), e.getMessage());
                    continue;
                }
            }
            if (matches) {
                log.debug("{}Pipe matched, executing handler", logLineHeader());
                try {
                    Object handlerResult = pipe.handler().evaluate().supply().orElse(null);
                    pipe.code().ifPresent(context::setCode);
                    pipe.variableName().ifPresent(varName -> {
                        if (handlerResult != null) {
                            context.setVariable(varName, handlerResult);
                        }
                    });
                    return (ExecutionReturned) handlerResult;
                } catch (Exception e) {
                    log.warn("{}Pipe handler execution failed: {}", logLineHeader(), e.getMessage());
                    return currentResult;
                }
            }
        }
        return currentResult;
    }

    private IRuntimeStepCatch findMatchingCatch(Throwable exception) {
        for (IRuntimeStepCatch stepCatch : this.catches) {
            Optional<? extends Throwable> cause = CoreException.findFirstInException(exception, stepCatch.exception());
            if (cause.isPresent()) {
                log.debug("{}Matching catch found for exception: {}", logLineHeader(),
                        cause.get().getClass().getSimpleName());
                return stepCatch;
            }
        }
        log.trace("{}No matching catch found", logLineHeader());
        return null;
    }

    private String logLineHeader() {
        return "[Runtime " + runtimeName + "][Step " + stepName + "][Expression "
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
