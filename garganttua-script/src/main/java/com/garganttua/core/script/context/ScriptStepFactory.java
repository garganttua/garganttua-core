package com.garganttua.core.script.context;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.context.IScriptFunction;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.CatchAwareExpression;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.IRuntimeStepPipe;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.RuntimeStepCatch;
import com.garganttua.core.runtime.RuntimeStepFallbackBinder;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.runtime.RuntimeStepOnException;
import com.garganttua.core.runtime.RuntimeStepPipe;
import com.garganttua.core.runtime.SubRuntime;
import com.garganttua.core.runtime.SubRuntimeExpression;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.FunctionDefNode;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.script.nodes.ScriptFunction;
import com.garganttua.core.script.nodes.StatementBlock;
import com.garganttua.core.script.nodes.StatementGroupNode;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

/**
 * Compiles {@link IScriptNode} AST nodes into {@link RuntimeStep} instances.
 *
 * <p>
 * This is the script-to-runtime compiler. Each script statement is mapped to
 * a standard {@code RuntimeStep} with a {@code RuntimeStepMethodBinder} whose
 * expression is the script expression. The runtime handles variable assignment,
 * output management, exception handling, and pipe evaluation.
 * </p>
 *
 * <p>
 * Script-specific features are mapped as follows:
 * <ul>
 *   <li>{@code var <- expr} → variable + expression</li>
 *   <li>{@code output <- expr} → variable("output") + isOutput</li>
 *   <li>{@code var = expr} → lazy expression (stores supplier)</li>
 *   <li>{@code expr ! ExType => handler} → {@link IRuntimeStepCatch} + fallback</li>
 *   <li>{@code expr | cond => handler} → {@link IRuntimeStepPipe}</li>
 *   <li>{@code (statements)} → {@link SubRuntimeExpression}</li>
 *   <li>{@code func = (p) => (body)} → function definition expression</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class ScriptStepFactory {

    private static final String RUNTIME_NAME = "script";

    /**
     * Compiles a list of script AST nodes into runtime steps.
     *
     * @param statements the AST nodes to compile
     * @return ordered map of step name → runtime step
     */
    public Map<String, IRuntimeStep<?, Object[], Object>> compile(List<IScriptNode> statements) {
        Map<String, IRuntimeStep<?, Object[], Object>> steps = new LinkedHashMap<>();
        for (int i = 0; i < statements.size(); i++) {
            String stepName = "step-" + i;
            steps.put(stepName, compileNode(stepName, statements.get(i)));
        }
        return steps;
    }

    @SuppressWarnings("unchecked")
    private RuntimeStep<Object, Object[], Object> compileNode(String stepName, IScriptNode node) {
        IExpression<Object, ISupplier<Object>> expression;

        if (node instanceof StatementGroupNode groupNode) {
            expression = compileGroup(stepName, groupNode);
        } else if (node instanceof FunctionDefNode funcDef) {
            expression = compileFunctionDef(funcDef);
        } else if (node.assignExpression() && node.variableName() != null) {
            // Lazy assignment: store the supplier itself, don't evaluate
            expression = compileLazy(node);
        } else {
            // Eager execution: evaluate and return result
            expression = wrapExpression((IExpression<Object, ISupplier<Object>>) (IExpression<?, ?>) node.expression());
        }

        // Wrap with code setting on success (before catches, so catch can override)
        if (node.code() != null) {
            expression = wrapWithCodeSetting(expression, node.code());
        }

        // Build pipe list
        List<IRuntimeStepPipe> pipes = compilePipes(node.pipeClauses());

        // Determine variable/output
        Optional<String> variable = Optional.ofNullable(node.variableName());
        boolean isOutput = "output".equals(node.variableName());

        // Immediate catches: wrap expression in CatchAwareExpression.
        // The CatchAwareExpression catches exceptions, executes the handler,
        // records the exception as aborted (which stops the chain), and returns
        // the handler's result.
        boolean hasCatches = !node.catchClauses().isEmpty();
        if (hasCatches) {
            expression = wrapWithCatches(expression, node.catchClauses());
        }

        // Build method binder
        String ref = "step " + stepName + ", line " + node.line();
        RuntimeStepMethodBinder<Object, Object[], Object> methodBinder = new RuntimeStepMethodBinder<>(
                RUNTIME_NAME, stepName, expression,
                variable, isOutput,
                0, // code handled by expression wrapper / catch / pipe
                Set.of(),
                pipes,
                Optional.empty(), // no step-level condition
                !hasCatches,  // abortOnUncatchedException: false when catches handle it
                true,  // nullable
                ref
        );

        // Build fallback for downstream catches only
        Optional<com.garganttua.core.runtime.IRuntimeStepFallbackBinder<Object, IRuntimeContext<Object[], Object>, Object[], Object>> fallback =
                compileFallback(stepName, List.of(), node.downstreamCatchClauses());

        return new RuntimeStep<>(RUNTIME_NAME, stepName, Object.class, methodBinder, fallback);
    }

    // --- Expression compilation ---

    @SuppressWarnings("unchecked")
    private IExpression<Object, ISupplier<Object>> wrapExpression(IExpression<Object, ISupplier<Object>> expr) {
        return new ScriptExpressionWrapper<>(expr);
    }

    @SuppressWarnings("unchecked")
    private IExpression<Object, ISupplier<Object>> compileGroup(String stepName, StatementGroupNode groupNode) {
        // Recursively compile inner statements as steps
        List<IScriptNode> stmts = groupNode.statements();
        Map<String, IRuntimeStep<?, Object[], Object>> innerSteps = new LinkedHashMap<>();
        Set<String> functionNames = new HashSet<>();

        for (int i = 0; i < stmts.size(); i++) {
            IScriptNode inner = stmts.get(i);
            String innerStepName = stepName + "-g" + i;

            if (i == stmts.size() - 1 && !(inner instanceof FunctionDefNode)) {
                // Last statement: force isOutput=true so the group returns its result
                innerSteps.put(innerStepName, compileNodeAsOutput(innerStepName, inner));
            } else {
                innerSteps.put(innerStepName, compileNode(innerStepName, inner));
            }
            if (inner instanceof FunctionDefNode funcDef) {
                functionNames.add(funcDef.variableName());
            }
        }

        SubRuntime<Object[], Object> subRuntime = new SubRuntime<>(stepName + "-group", innerSteps);
        return (IExpression<Object, ISupplier<Object>>) (IExpression<?, ?>)
                new SubRuntimeExpression(subRuntime, functionNames);
    }

    /**
     * Compiles a node like compileNode but forces isOutput=true so its result
     * becomes the sub-runtime's output (used for the last statement in a group).
     */
    @SuppressWarnings("unchecked")
    private RuntimeStep<Object, Object[], Object> compileNodeAsOutput(String stepName, IScriptNode node) {
        IExpression<Object, ISupplier<Object>> expression;

        if (node instanceof StatementGroupNode groupNode) {
            expression = compileGroup(stepName, groupNode);
        } else if (node.assignExpression() && node.variableName() != null) {
            expression = compileLazy(node);
        } else {
            expression = wrapExpression((IExpression<Object, ISupplier<Object>>) (IExpression<?, ?>) node.expression());
        }

        if (node.code() != null) {
            expression = wrapWithCodeSetting(expression, node.code());
        }

        boolean hasCatches = !node.catchClauses().isEmpty();
        if (hasCatches) {
            expression = wrapWithCatches(expression, node.catchClauses());
        }

        List<IRuntimeStepPipe> pipes = compilePipes(node.pipeClauses());
        Optional<String> variable = Optional.ofNullable(node.variableName());
        String ref = "step " + stepName + ", line " + node.line();

        RuntimeStepMethodBinder<Object, Object[], Object> methodBinder = new RuntimeStepMethodBinder<>(
                RUNTIME_NAME, stepName, expression,
                variable, true, // isOutput=true — last statement in a group
                0, Set.of(), pipes, Optional.empty(), !hasCatches, true, ref
        );

        var fallback = compileFallback(stepName, node.catchClauses(), node.downstreamCatchClauses());
        return new RuntimeStep<>(RUNTIME_NAME, stepName, Object.class, methodBinder, fallback);
    }

    @SuppressWarnings("unchecked")
    private IExpression<Object, ISupplier<Object>> compileFunctionDef(FunctionDefNode funcDef) {
        // Expression that resolves the body block and creates a ScriptFunction
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException {
                        IRuntimeContext<?, ?> ctx = RuntimeExpressionContext.get();
                        if (ctx == null) {
                            throw new SupplyException("No runtime context for function definition");
                        }
                        Optional<StatementBlock> body = ctx.getVariable(funcDef.bodyBlockName(),
                                IClass.getClass(StatementBlock.class));
                        if (body.isEmpty()) {
                            throw new SupplyException("Function body block not found: " + funcDef.bodyBlockName());
                        }
                        IScriptFunction func = new ScriptFunction(
                                funcDef.variableName(), funcDef.parameterNames(), body.get());
                        return Optional.of(func);
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
    }

    @SuppressWarnings("unchecked")
    private IExpression<Object, ISupplier<Object>> compileLazy(IScriptNode node) {
        // Returns the supplier itself as the result value (not evaluated)
        IExpression<?, ? extends ISupplier<?>> innerExpr = node.expression();
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() throws SupplyException {
                        try {
                            ISupplier<?> supplier = innerExpr.evaluate();
                            return Optional.of(supplier);
                        } catch (ExpressionException e) {
                            throw new SupplyException(e);
                        }
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
    }

    // --- Code setting ---

    private IExpression<Object, ISupplier<Object>> wrapWithCodeSetting(
            IExpression<Object, ISupplier<Object>> inner, int codeValue) {
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() throws ExpressionException {
                ISupplier<Object> innerSupplier = inner.evaluate();
                return new ISupplier<>() {
                    @Override
                    @SuppressWarnings("rawtypes")
                    public Optional<Object> supply() throws SupplyException {
                        // Save context before inner evaluation — sub-scripts may clear it
                        IRuntimeContext ctx = RuntimeExpressionContext.get();
                        Optional<Object> result = innerSupplier.supply();
                        // Set code on success
                        if (ctx != null) {
                            ctx.setCode(codeValue);
                        }
                        return result;
                    }

                    @Override
                    public Type getSuppliedType() { return inner.getSuppliedType(); }

                    @Override
                    public IClass<Object> getSuppliedClass() { return inner.getSuppliedClass(); }
                };
            }

            @Override
            public Type getSuppliedType() { return inner.getSuppliedType(); }

            @Override
            public IClass<Object> getSuppliedClass() { return inner.getSuppliedClass(); }

            @Override
            public boolean isContextual() { return inner.isContextual(); }
        };
    }

    // --- Catch compilation ---

    /**
     * Wraps an expression with catch-and-stop semantics using CatchAwareExpression.
     * When a catch matches, the handler executes, the code is set, and the exception
     * is recorded as aborted in the context (which stops the chain).
     */
    /**
     * Wraps an expression with catch-and-stop semantics.
     * Uses the original CatchClause.matches() for exception matching (supports
     * class resolution, simple name, and FQCN matching).
     */
    @SuppressWarnings("unchecked")
    private IExpression<Object, ISupplier<Object>> wrapWithCatches(
            IExpression<Object, ISupplier<Object>> inner, List<CatchClause> catchClauses) {
        // Build CatchHandlers that delegate matching to CatchClause.matches()
        List<CatchAwareExpression.CatchHandler<Object>> handlers = new ArrayList<>();
        for (CatchClause cc : catchClauses) {
            IExpression<Object, ISupplier<Object>> handlerExpr = cc.handler() != null
                    ? wrapExpression((IExpression<Object, ISupplier<Object>>) (IExpression<?, ?>) cc.handler().expression())
                    : nullExpression();

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

    /**
     * Compiles catch clauses into IRuntimeStepCatch entries.
     * For catch clauses with unresolvable exception types (e.g., by simple name),
     * a catch-all (Throwable) is used — the fallback expression dispatches by
     * CatchClause.matches() which supports name-based matching.
     */
    @SuppressWarnings("unchecked")
    private Set<IRuntimeStepCatch> compileCatches(List<CatchClause> catchClauses) {
        if (catchClauses == null || catchClauses.isEmpty()) {
            return Set.of();
        }
        Set<IRuntimeStepCatch> catches = new HashSet<>();
        IClass<? extends Throwable> throwableClass =
                (IClass<? extends Throwable>) (IClass<?>) IClass.getClass(Throwable.class);

        for (CatchClause cc : catchClauses) {
            Integer catchCode = cc.code() != null ? cc.code()
                    : (cc.handler() != null ? cc.handler().code() : null);
            int code = catchCode != null ? catchCode : IRuntime.GENERIC_RUNTIME_ERROR_CODE;

            List<IClass<? extends Throwable>> types = resolveExceptionTypes(cc.exceptionTypes());
            if (types.isEmpty()) {
                // Catch-all OR unresolvable types → match Throwable
                // Actual dispatching happens in the fallback via CatchClause.matches()
                catches.add(new RuntimeStepCatch(throwableClass, code));
            } else {
                for (IClass<? extends Throwable> type : types) {
                    catches.add(new RuntimeStepCatch(type, code));
                }
            }
        }
        return catches;
    }

    @SuppressWarnings("unchecked")
    private List<IClass<? extends Throwable>> resolveExceptionTypes(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) {
            return List.of();
        }
        List<IClass<? extends Throwable>> result = new ArrayList<>();
        for (String name : typeNames) {
            try {
                result.add((IClass<? extends Throwable>) (IClass<?>) IClass.forName(name));
            } catch (ClassNotFoundException e) {
                log.atDebug().log("Exception type '{}' not found as class, skipping", name);
            }
        }
        return result;
    }

    // --- Pipe compilation ---

    @SuppressWarnings("unchecked")
    private List<IRuntimeStepPipe> compilePipes(List<PipeClause> pipeClauses) {
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
                    : nullExpression();

            Integer pipeCode = pc.code() != null ? pc.code()
                    : (pc.handler() != null ? pc.handler().code() : null);
            String pipeVar = pc.handler() != null ? pc.handler().variableName() : null;
            pipes.add(new RuntimeStepPipe(condition, handler, Optional.ofNullable(pipeCode),
                    Optional.ofNullable(pipeVar)));
        }
        return pipes;
    }

    // --- Fallback compilation (immediate + downstream catches) ---

    /**
     * Builds a single fallback binder that handles both immediate and downstream catches.
     * The fallback expression dispatches to the correct handler based on exception matching.
     */
    @SuppressWarnings("unchecked")
    private Optional<com.garganttua.core.runtime.IRuntimeStepFallbackBinder<Object, IRuntimeContext<Object[], Object>, Object[], Object>>
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
                                                wrapExpression((IExpression<Object, ISupplier<Object>>)
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
                RUNTIME_NAME, stepName, dispatchExpr,
                Optional.empty(), false, onExceptions, true,
                "downstream-catch:" + stepName);

        return Optional.of(fallback);
    }

    // --- Utilities ---

    private static IExpression<Object, ISupplier<Object>> nullExpression() {
        return new IExpression<>() {
            @Override
            public ISupplier<Object> evaluate() {
                return new ISupplier<>() {
                    @Override
                    public Optional<Object> supply() { return Optional.empty(); }

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
    }
}
