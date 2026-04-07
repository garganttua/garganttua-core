package com.garganttua.core.runtime;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.CoreException;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

/**
 * Expression decorator that handles immediate catch clauses ("catch and resume").
 *
 * <p>
 * Wraps an inner expression with a list of catch handlers. If the inner expression
 * throws an exception matching a handler's type, the handler expression is evaluated
 * and its result is returned instead. If no handler matches, the exception propagates.
 * </p>
 *
 * <p>
 * This implements the script's {@code expression ! ExType => handler} semantics,
 * where execution resumes normally after the handler. This is distinct from the
 * runtime's {@code @Catch} mechanism which records the exception and aborts.
 * </p>
 *
 * @param <R> the result type
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class CatchAwareExpression<R> implements IExpression<R, ISupplier<R>> {

    /**
     * A single catch clause: matcher + handler expression + optional code.
     */
    public static class CatchHandler<R> {
        private final java.util.function.Predicate<Throwable> matcher;
        private final IExpression<R, ? extends ISupplier<R>> handler;
        private final Optional<Integer> code;
        private final String variableName;

        public CatchHandler(List<IClass<? extends Throwable>> exceptionTypes,
                IExpression<R, ? extends ISupplier<R>> handler, Optional<Integer> code) {
            this(exceptionTypes, handler, code, null);
        }

        public CatchHandler(List<IClass<? extends Throwable>> exceptionTypes,
                IExpression<R, ? extends ISupplier<R>> handler, Optional<Integer> code, String variableName) {
            this.handler = handler;
            this.code = code;
            this.variableName = variableName;
            if (exceptionTypes == null || exceptionTypes.isEmpty()) {
                this.matcher = t -> true;
            } else {
                this.matcher = t -> exceptionTypes.stream()
                        .anyMatch(type -> CoreException.findFirstInException(t, type).isPresent());
            }
        }

        public CatchHandler(java.util.function.Predicate<Throwable> matcher,
                IExpression<R, ? extends ISupplier<R>> handler, Optional<Integer> code, String variableName) {
            this.matcher = matcher;
            this.handler = handler;
            this.code = code;
            this.variableName = variableName;
        }

        public boolean matches(Throwable exception) {
            return matcher.test(exception);
        }

        public IExpression<R, ? extends ISupplier<R>> handler() { return handler; }
        public Optional<Integer> code() { return code; }
        public String variableName() { return variableName; }
    }

    /**
     * Thrown when a catch handler matches and executes successfully.
     * Carries the handler's result so the caller can extract it and stop the chain.
     */
    public static class CatchResultException extends RuntimeException {
        private final Object result;
        private final String variableName;
        public CatchResultException(Object result, String variableName) {
            super("Catch handler matched");
            this.result = result;
            this.variableName = variableName;
        }
        public Object getResult() { return result; }
        public String getVariableName() { return variableName; }
    }

    private final IExpression<R, ? extends ISupplier<R>> inner;
    private final List<CatchHandler<R>> handlers;

    public CatchAwareExpression(IExpression<R, ? extends ISupplier<R>> inner, List<CatchHandler<R>> handlers) {
        this.inner = Objects.requireNonNull(inner, "Inner expression cannot be null");
        this.handlers = List.copyOf(Objects.requireNonNull(handlers, "Handlers cannot be null"));
    }

    @Override
    public ISupplier<R> evaluate() throws ExpressionException {
        return new CatchAwareSupplier();
    }

    @Override
    public Type getSuppliedType() {
        return inner.getSuppliedType();
    }

    @Override
    public IClass<R> getSuppliedClass() {
        return inner.getSuppliedClass();
    }

    @Override
    public boolean isContextual() {
        return inner.isContextual();
    }

    private class CatchAwareSupplier implements ISupplier<R> {

        @Override
        public Optional<R> supply() throws SupplyException {
            try {
                return inner.evaluate().supply();
            } catch (Exception e) {
                // Match against the full exception (not just cause) — CatchClause.matches()
                // checks both the exception itself and its cause chain
                // Save context — handler evaluation might clear it (sub-scripts)
                IRuntimeContext<?, ?> savedCtx = RuntimeExpressionContext.get();
                for (CatchHandler<R> handler : handlers) {
                    if (handler.matches(e) || (e.getCause() != null && handler.matches(e.getCause()))) {
                        log.atDebug().log("Catch handler matched for {}, executing handler",
                                e.getClass().getSimpleName());
                        try {
                            // Set exception context variables for handler access (@exception, @message)
                            if (savedCtx != null) {
                                Throwable actualCause = e.getCause() != null ? e.getCause() : e;
                                savedCtx.setVariable("exception", actualCause);
                                savedCtx.setVariable("message", actualCause.getMessage());
                            }
                            // Set code if specified
                            handler.code().ifPresent(code -> {
                                if (savedCtx != null) {
                                    savedCtx.setCode(code);
                                }
                            });
                            // Execute handler, then throw to stop the chain
                            Optional<R> handlerResult = handler.handler().evaluate().supply();
                            // Wrap result in a CatchResult exception — the RuntimeStepMethodBinder
                            // will detect this, extract the result, and stop the chain
                            throw new CatchResultException(handlerResult.orElse(null), handler.variableName());
                        } catch (CatchResultException cre) {
                            throw cre; // rethrow — handled by caller
                        } catch (Exception handlerEx) {
                            throw new SupplyException("Catch handler failed", handlerEx);
                        }
                    }
                }
                // No handler matched — propagate
                throw e instanceof SupplyException se ? se : new SupplyException(e);
            }
        }

        @Override
        public Type getSuppliedType() {
            return inner.getSuppliedType();
        }

        @Override
        public IClass<R> getSuppliedClass() {
            return inner.getSuppliedClass();
        }
    }
}
