package com.garganttua.core.script.context;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.expression.ExpressionException;
import com.garganttua.core.expression.IExpression;
import com.garganttua.core.expression.context.IScriptFunction;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.CatchAwareExpression;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.IRuntimeStepCatch;
import com.garganttua.core.runtime.IRuntimeStepPipe;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.runtime.RuntimeStep;
import com.garganttua.core.runtime.RuntimeStepMethodBinder;
import com.garganttua.core.runtime.SubRuntime;
import com.garganttua.core.runtime.SubRuntimeExpression;
import com.garganttua.core.script.nodes.FunctionDefNode;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.ScriptFunction;
import com.garganttua.core.script.nodes.StatementBlock;
import com.garganttua.core.script.nodes.StatementGroupNode;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

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
public class ScriptStepFactory {
    private static final Logger log = Logger.getLogger(ScriptStepFactory.class);

    static final String RUNTIME_NAME = "script";

    /**
     * Compiles a list of script AST nodes into runtime steps.
     *
     * @param statements the AST nodes to compile
     * @return ordered map of step name → runtime step
     */
    public Map<String, IRuntimeStep<?, Object[], Object>> compile(List<IScriptNode> statements) {
        Map<String, IRuntimeStep<?, Object[], Object>> steps = new LinkedHashMap<>();
        for (int i = 0; i < statements.size(); i++) {
            String stepName = buildStepName(i, statements.get(i));
            // Defend against duplicate descriptors (two `result <- ...` lines share
            // the same variableName). Suffix with an index in that rare case.
            String unique = stepName;
            int dedup = 2;
            while (steps.containsKey(unique)) {
                unique = stepName + "#" + dedup++;
            }
            steps.put(unique, compileNode(unique, statements.get(i)));
        }
        return steps;
    }

    /**
     * Build a meaningful, deterministic step name for observability labels and
     * logs. Format: {@code step-<index>[-<descriptor>]}.
     *
     * <p>The descriptor is — in order of preference:
     * <ul>
     *   <li>the assigned variable name (e.g. {@code step-0-user} for
     *       {@code user <- fetchUser()}), or the function name for a function
     *       definition (both expose it via {@link IScriptNode#variableName()});</li>
     *   <li>the literal {@code group} for an unnamed statement group;</li>
     *   <li>omitted for a plain side-effect expression (just {@code step-<index>}).</li>
     * </ul>
     * Names are sanitized to keep only {@code [a-zA-Z0-9_.]}; everything else
     * is collapsed to {@code _}. This keeps the observability source string
     * safe for downstream tooling (Prometheus labels, JSON keys, etc.).
     */
    private static String buildStepName(int index, IScriptNode node) {
        String descriptor = describe(node);
        if (descriptor.isEmpty()) {
            return "step-" + index;
        }
        return "step-" + index + "-" + sanitize(descriptor);
    }

    private static String describe(IScriptNode node) {
        String var = node.variableName();
        if (var != null && !var.isBlank()) {
            return var;
        }
        if (node instanceof com.garganttua.core.script.nodes.StatementGroupNode) {
            return "group";
        }
        return "";
    }

    private static String sanitize(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9_.]", "_");
    }

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
        List<IRuntimeStepPipe> pipes = ScriptClauseCompiler.compilePipes(node.pipeClauses());

        // Determine variable/output
        Optional<String> variable = Optional.ofNullable(node.variableName());
        boolean isOutput = "output".equals(node.variableName());

        // Immediate catches: wrap expression in CatchAwareExpression.
        // The CatchAwareExpression catches exceptions, executes the handler,
        // records the exception as aborted (which stops the chain), and returns
        // the handler's result.
        boolean hasCatches = !node.catchClauses().isEmpty();
        if (hasCatches) {
            expression = ScriptClauseCompiler.wrapWithCatches(expression, node.catchClauses());
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
                ScriptClauseCompiler.compileFallback(stepName, List.of(), node.downstreamCatchClauses());

        return new RuntimeStep<>(RUNTIME_NAME, stepName, Object.class, methodBinder, fallback);
    }

    // --- Expression compilation ---

    static IExpression<Object, ISupplier<Object>> wrapExpression(IExpression<Object, ISupplier<Object>> expr) {
        return new ScriptExpressionWrapper<>(expr);
    }

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
            expression = ScriptClauseCompiler.wrapWithCatches(expression, node.catchClauses());
        }

        List<IRuntimeStepPipe> pipes = ScriptClauseCompiler.compilePipes(node.pipeClauses());
        Optional<String> variable = Optional.ofNullable(node.variableName());
        String ref = "step " + stepName + ", line " + node.line();

        RuntimeStepMethodBinder<Object, Object[], Object> methodBinder = new RuntimeStepMethodBinder<>(
                RUNTIME_NAME, stepName, expression,
                variable, true, // isOutput=true — last statement in a group
                0, Set.of(), pipes, Optional.empty(), !hasCatches, true, ref
        );

        var fallback = ScriptClauseCompiler.compileFallback(stepName, node.catchClauses(), node.downstreamCatchClauses());
        return new RuntimeStep<>(RUNTIME_NAME, stepName, Object.class, methodBinder, fallback);
    }

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


    // --- Utilities ---

    static IExpression<Object, ISupplier<Object>> nullExpression() {
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
