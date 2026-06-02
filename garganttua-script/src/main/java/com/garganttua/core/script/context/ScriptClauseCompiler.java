package com.garganttua.core.script.context;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.CatchAwareExpression;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStepPipe;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.runtime.RuntimeStepFallbackBinder;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.RuntimeStepPipe;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Compiles a script statement's error-handling clauses — catch wrapping, pipe
 * clauses and the downstream-catch fallback dispatch — into runtime constructs.
 * Stateless; extracted from {@link ScriptStepFactory} to keep that compiler focused
 * on the core statement-to-step mapping.
 */
final class ScriptClauseCompiler {
    private static final Logger log = Logger.getLogger(ScriptClauseCompiler.class);
    private ScriptClauseCompiler() {}

    @SuppressWarnings("unchecked")
    static IExpression<Object, ISupplier<Object>> wrapWithCatches(
            IExpression<Object, ISupplier<Object>> inner, List<CatchClause> catchClauses) {
        // Build CatchHandlers that delegate matching to CatchClause.matches()
        List<CatchAwareExpression.CatchHandler<Object>> handlers = new ArrayList<>();
        for (CatchClause cc : catchClauses) {
            IExpression<Object, ISupplier<Object>> handlerExpr = cc.handler() != null
                    ? ScriptStepFactory.wrapExpression((IExpression<Object, ISupplier<Object>>) (IExpression<?, ?>) cc.handler().expression())
                    : ScriptStepFactory.nullExpression();

            Integer catchCode = cc.code() != null ? cc.code()
                    : (cc.handler() != null ? cc.handler().code() : null);

            // Delegate matching to CatchClause.matches() which supports
            // class resolution, simple name, and FQCN matching
            String handlerVar = cc.handler() != null ? cc.handler().variableName() : null;
            handlers.add(new CatchAwareExpression.CatchHandler<>(
                    cc::matches, handlerExpr, Optional.ofNullable(catchCode), handlerVar));
        }
        return new CatchAwareExpression<>(inner, handlers);
    }

    @SuppressWarnings("unchecked")
    static List<IRuntimeStepPipe> compilePipes(List<PipeClause> pipeClauses) {
        if (pipeClauses == null || pipeClauses.isEmpty()) {
            return List.of();
        }
        List<IRuntimeStepPipe> pipes = new ArrayList<>();
        for (PipeClause pc : pipeClauses) {
            Optional<IExpression<Boolean, ? extends ISupplier<Boolean>>> condition = pc.isDefault()
                    ? Optional.empty()
                    : Optional.of(new ScriptExpressionWrapper<>(
                            (IExpression<Boolean, ? extends ISupplier<Boolean>>) (IExpression<?, ?>) pc.condition()));

            IExpression<?, ? extends ISupplier<?>> handler = pc.handler() != null
                    ? new ScriptExpressionWrapper<>((IExpression) pc.handler().expression())
                    : ScriptStepFactory.nullExpression();

            Integer pipeCode = pc.code() != null ? pc.code()
                    : (pc.handler() != null ? pc.handler().code() : null);
            String pipeVar = pc.handler() != null ? pc.handler().variableName() : null;
            pipes.add(new RuntimeStepPipe(condition, handler, Optional.ofNullable(pipeCode),
                    Optional.ofNullable(pipeVar)));
        }
        return pipes;
    }

    @SuppressWarnings("unchecked")
    static Optional<com.garganttua.core.runtime.IRuntimeStepFallbackBinder<Object, IRuntimeContext<Object[], Object>, Object[], Object>>
            compileFallback(String stepName, List<CatchClause> immediateClauses, List<CatchClause> downstreamClauses) {

        // Merge all clauses that have handlers
        List<CatchClause> allClauses = new ArrayList<>();
        if (immediateClauses != null) allClauses.addAll(immediateClauses);
        if (downstreamClauses != null) allClauses.addAll(downstreamClauses);

        if (allClauses.isEmpty()) {
            return Optional.empty();
        }

        // Create a dispatching expression that checks which clause matches
        IExpression<Object, ISupplier<Object>> dispatchExpr = new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException {
                        IRuntimeContext<?, ?> ctx = RuntimeExpressionContext.get();
                        if (ctx == null) return Optional.empty();
                        var abortingEx = ctx.findAbortingExceptionReport();
                        if (abortingEx.isEmpty()) return Optional.empty();
                        Throwable cause = abortingEx.get().exception();

                        for (CatchClause cc : allClauses) {
                            if (cc.matches(cause)) {
                                // Set code from catch clause or handler
                                Integer catchCode = cc.code() != null ? cc.code()
                                        : (cc.handler() != null ? cc.handler().code() : null);
                                if (catchCode != null) {
                                    ctx.setCode(catchCode);
                                }
                                // Execute handler if present
                                if (cc.handler() != null) {
                                    try {
                                        IExpression<Object, ISupplier<Object>> handlerExpr =
                                                ScriptStepFactory.wrapExpression((IExpression<Object, ISupplier<Object>>)
                                                        (IExpression<?, ?>) cc.handler().expression());
                                        Optional<Object> handlerResult = (Optional<Object>) (Optional<?>) handlerExpr.evaluate().supply();
                                        // Set handler variable if present
                                        String handlerVar = cc.handler().variableName();
                                        if (handlerVar != null && handlerResult.isPresent()) {
                                            ctx.setVariable(handlerVar, handlerResult.get());
                                        }
                                        return handlerResult;
                                    } catch (Exception e) {
                                        throw new SupplyException("Catch handler failed", e);
                                    }
                                }
                                return Optional.empty();
                            }
                        }
                        return Optional.empty();
                    }

                    @Override
                    public Type getSuppliedType() { return Object.class; }

                    @Override
                    public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }
                };
            }

            @Override
            public Type getSuppliedType() { return Object.class; }

            @Override
            public IClass<Object> getSuppliedClass() { return IClass.getClass(Object.class); }

            @Override
            public boolean isContextual() { return false; }
        };

        // Accept any exception (dispatching happens inside the expression)
        List<com.garganttua.core.runtime.IRuntimeStepOnException> onExceptions = List.of(
                new RuntimeStepOnException((IClass<? extends Throwable>) (IClass<?>) IClass.getClass(Throwable.class), null, null));

        RuntimeStepFallbackBinder<Object, Object[], Object> fallback = new RuntimeStepFallbackBinder<>(
                ScriptStepFactory.RUNTIME_NAME, stepName, dispatchExpr,
                Optional.empty(), false, onExceptions, true,
                "downstream-catch:" + stepName);

        return Optional.of(fallback);
    }
}
