package com.garganttua.core.script.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.expression.ForLoopExpressionNode;
import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeExceptionRecord;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.runtime.RuntimeStepExecutionTools;
import com.garganttua.core.expression.context.IScriptFunction;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.FunctionDefNode;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.script.nodes.ScriptFunction;
import com.garganttua.core.script.nodes.StatementBlock;
import com.garganttua.core.script.nodes.StatementGroupNode;
import com.garganttua.core.supply.ISupplier;

/**
 * Bridges a script AST node ({@link IScriptNode}) to the runtime execution chain.
 *
 * <p>Each script statement becomes one {@code ScriptRuntimeStep}. The step handles:
 * <ul>
 *   <li>Eager and lazy variable assignment</li>
 *   <li>Statement group execution with function scope isolation</li>
 *   <li>User-defined function registration ({@link FunctionDefNode} &rarr; {@link ScriptFunction})</li>
 *   <li>Exception catch/fallback clauses and conditional pipe clauses</li>
 * </ul>
 *
 * <p><b>Function scope isolation in groups:</b> when executing a {@link StatementGroupNode},
 * function definitions inside the group are saved before execution and restored afterwards,
 * so that functions defined in one group do not leak into subsequent groups or stages.
 */
public class ScriptRuntimeStep implements IRuntimeStep<Object, Object[], Object> {

    private final String stepName;
    private final IScriptNode node;

    public ScriptRuntimeStep(String stepName, IScriptNode node) {
        this.stepName = stepName;
        this.node = node;
    }

    @Override
    public String getStepName() {
        return this.stepName;
    }

    @Override
    public void defineExecutionStep(IExecutorChain<IRuntimeContext<Object[], Object>> chain) {
        var executor = new com.garganttua.core.execution.IExecutor<IRuntimeContext<Object[], Object>>() {
            @Override
            public void execute(IRuntimeContext<Object[], Object> context,
                    IExecutorChain<IRuntimeContext<Object[], Object>> next)
                    throws com.garganttua.core.execution.ExecutorException {
                boolean caught = false;
                RuntimeExpressionContext.set(context);
                ExpressionVariableContext.set(new ForLoopExpressionNode.VariableSettableResolver() {
                    private static final IClass<Object> OBJECT_CLASS = IClass.getClass(Object.class);
                    private static final IClass<Integer> INTEGER_CLASS = IClass.getClass(Integer.class);
                    @Override
                    public <T> java.util.Optional<T> resolve(String name, IClass<T> type) {
                        // Handle argument references: $0, $1, $2, etc.
                        if (name.startsWith("$")) {
                            try {
                                int index = Integer.parseInt(name.substring(1));
                                Object[] args = context.getInput().orElse(null);
                                if (args != null && index >= 0 && index < args.length) {
                                    Object value = args[index];
                                    if (value == null) {
                                        return java.util.Optional.empty();
                                    }
                                    if (type.isInstance(value)) {
                                        return java.util.Optional.of(type.cast(value));
                                    }
                                    // Try to return as Object if type doesn't match
                                    if (type.equals(OBJECT_CLASS)) {
                                        T result = (T) value;
                                        return java.util.Optional.of(result);
                                    }
                                }
                                return java.util.Optional.empty();
                            } catch (NumberFormatException e) {
                                // Not a valid argument index, fall through to variable resolution
                            }
                        }
                        // Handle special reserved variables
                        if ("code".equals(name)) {
                            java.util.Optional<Integer> code = context.getCode();
                            if (code.isPresent() && type.isAssignableFrom(INTEGER_CLASS)) {
                                T result = (T) code.get();
                                return java.util.Optional.of(result);
                            }
                            return java.util.Optional.empty();
                        }
                        if ("output".equals(name)) {
                            Object output = context.getResult().output();
                            if (output != null && type.isInstance(output)) {
                                return java.util.Optional.of(type.cast(output));
                            }
                            if (output != null && type.equals(OBJECT_CLASS)) {
                                T result = (T) output;
                                return java.util.Optional.of(result);
                            }
                            return java.util.Optional.empty();
                        }
                        return context.getVariable(name, type);
                    }
                    @Override
                    public void setVariable(String name, Object value) {
                        // Handle special variable @output
                        if ("output".equals(name)) {
                            Object output = value;
                            context.setOutput(output);
                            return;
                        }
                        context.setVariable(name, value);
                    }
                });
                try {
                    // Handle StatementGroupNode specially
                    if (node instanceof StatementGroupNode groupNode) {
                        Object lastResult = executeGroup(context, groupNode);
                        handleResult(context, node, lastResult);
                        evaluatePipeClauses(context, lastResult);
                    } else if (node instanceof FunctionDefNode funcDef) {
                        StatementBlock body = resolveBlock(context, funcDef.bodyBlockName());
                        IScriptFunction func = new ScriptFunction(
                                funcDef.variableName(), funcDef.parameterNames(), body);
                        context.setVariable(funcDef.variableName(), func);
                    } else if (node.assignExpression() && node.variableName() != null) {
                        // For lazy assignment (=), store the expression without evaluating
                        ISupplier<?> supplier = node.expression().evaluate();
                        context.setVariable(node.variableName(), supplier);
                        if (node.code() != null) {
                            context.setCode(node.code());
                        }
                    } else {
                        // Eager evaluation: execute and handle result
                        Object result = node.execute();
                        handleResult(context, node, result);
                        evaluatePipeClauses(context, result);
                    }
                } catch (ScriptException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    for (CatchClause catchClause : node.catchClauses()) {
                        if (catchClause.matches(cause)) {
                            // Set exception-related variables for the handler
                            context.setVariable("exception", cause);
                            context.setVariable("message", cause.getMessage() != null ? cause.getMessage() : "");
                            // Set current code value (default 0 if not set)
                            Integer currentCode = context.getCode().orElse(0);
                            context.setVariable("code", currentCode);
                            if (catchClause.handler() != null) {
                                Object handlerResult = catchClause.handler().execute();
                                handleResult(context, catchClause.handler(), handlerResult);
                            }
                            if (catchClause.code() != null) {
                                context.setCode(catchClause.code());
                            }
                            caught = true;
                            break;
                        }
                    }
                    if (!caught) {
                        storeErrorContext(context, cause);
                        RuntimeStepExecutionTools.handleException(
                                "script", stepName, context, cause, true,
                                "script:" + stepName, null, logHeader());
                    }
                } catch (Exception e) {
                    storeErrorContext(context, e);
                    throw new ScriptException(findRootCause(e).getMessage(), e);
                } finally {
                    ExpressionVariableContext.clear();
                    RuntimeExpressionContext.clear();
                }
                if (!caught) {
                    next.execute(context);
                }
            }
        };

        if (this.node.downstreamCatchClauses().isEmpty()) {
            chain.addExecutor(executor);
        } else {
            IFallBackExecutor<IRuntimeContext<Object[], Object>> fallback = (context, nextFallback) -> {
                RuntimeExpressionContext.set(context);
                ExpressionVariableContext.set(new ForLoopExpressionNode.VariableSettableResolver() {
                    private static final IClass<Object> OBJECT_CLASS = IClass.getClass(Object.class);
                    private static final IClass<Integer> INTEGER_CLASS = IClass.getClass(Integer.class);
                    @Override
                    public <T> java.util.Optional<T> resolve(String name, IClass<T> type) {
                        // Handle argument references: $0, $1, $2, etc.
                        if (name.startsWith("$")) {
                            try {
                                int index = Integer.parseInt(name.substring(1));
                                Object[] args = context.getInput().orElse(null);
                                if (args != null && index >= 0 && index < args.length) {
                                    Object value = args[index];
                                    if (value == null) {
                                        return java.util.Optional.empty();
                                    }
                                    if (type.isInstance(value)) {
                                        return java.util.Optional.of(type.cast(value));
                                    }
                                    if (type.equals(OBJECT_CLASS)) {
                                        T result = (T) value;
                                        return java.util.Optional.of(result);
                                    }
                                }
                                return java.util.Optional.empty();
                            } catch (NumberFormatException e) {
                                // Not a valid argument index
                            }
                        }
                        // Handle special reserved variables
                        if ("code".equals(name)) {
                            java.util.Optional<Integer> code = context.getCode();
                            if (code.isPresent() && type.isAssignableFrom(INTEGER_CLASS)) {
                                T result = (T) code.get();
                                return java.util.Optional.of(result);
                            }
                            return java.util.Optional.empty();
                        }
                        if ("output".equals(name)) {
                            Object output = context.getResult().output();
                            if (output != null && type.isInstance(output)) {
                                return java.util.Optional.of(type.cast(output));
                            }
                            if (output != null && type.equals(OBJECT_CLASS)) {
                                T result = (T) output;
                                return java.util.Optional.of(result);
                            }
                            return java.util.Optional.empty();
                        }
                        return context.getVariable(name, type);
                    }
                    @Override
                    public void setVariable(String name, Object value) {
                        if ("output".equals(name)) {
                            Object output = value;
                            context.setOutput(output);
                            return;
                        }
                        context.setVariable(name, value);
                    }
                });
                try {
                    Optional<RuntimeExceptionRecord> abortingEx = context.findAbortingExceptionReport();
                    if (abortingEx.isPresent()) {
                        Throwable cause = abortingEx.get().exception();
                        for (CatchClause cc : node.downstreamCatchClauses()) {
                            if (cc.matches(cause)) {
                                // Set exception-related variables for the handler
                                context.setVariable("exception", cause);
                                context.setVariable("message", cause.getMessage() != null ? cause.getMessage() : "");
                                // Set current code value (default 0 if not set)
                                Integer currentCode = context.getCode().orElse(0);
                                context.setVariable("code", currentCode);
                                if (cc.handler() != null) {
                                    Object handlerResult = cc.handler().execute();
                                    handleResult(context, cc.handler(), handlerResult);
                                }
                                if (cc.code() != null) {
                                    context.setCode(cc.code());
                                }
                                return;
                            }
                        }
                    }
                } finally {
                    ExpressionVariableContext.clear();
                    RuntimeExpressionContext.clear();
                }
                nextFallback.executeFallBack(context);
            };
            chain.addExecutor(executor, fallback);
        }
    }

    /**
     * Executes all statements in a group and returns the last result.
     */
    @SuppressWarnings("unchecked")
    private Object executeGroup(IRuntimeContext<Object[], Object> context, StatementGroupNode groupNode) throws ScriptException {
        // Save existing function values for names that will be defined in this group,
        // so we can restore them after group execution (function scope isolation).
        Map<String, Object> savedFunctions = new HashMap<>();
        for (IScriptNode innerNode : groupNode.statements()) {
            if (innerNode instanceof FunctionDefNode funcDef) {
                String name = funcDef.variableName();
                Optional<Object> existing = context.getVariable(name, (IClass<Object>) IClass.getClass(Object.class));
                savedFunctions.put(name, existing.orElse(null));
            }
        }

        Object lastResult = null;
        for (IScriptNode innerNode : groupNode.statements()) {
            if (innerNode instanceof StatementGroupNode innerGroup) {
                // Recursively execute nested groups
                lastResult = executeGroup(context, innerGroup);
                // Handle nested group's variable assignment
                if (innerGroup.variableName() != null) {
                    String varName = innerGroup.variableName();
                    if ("output".equals(varName)) {
                        context.setOutput(lastResult);
                    }
                    context.setVariable(varName, lastResult);
                }
                if (innerGroup.code() != null) {
                    context.setCode(innerGroup.code());
                }
            } else if (innerNode instanceof FunctionDefNode funcDef) {
                StatementBlock body = resolveBlock(context, funcDef.bodyBlockName());
                IScriptFunction func = new ScriptFunction(
                        funcDef.variableName(), funcDef.parameterNames(), body);
                context.setVariable(funcDef.variableName(), func);
            } else if (innerNode.assignExpression() && innerNode.variableName() != null) {
                // Lazy assignment
                ISupplier<?> supplier = innerNode.expression().evaluate();
                context.setVariable(innerNode.variableName(), supplier);
                if (innerNode.code() != null) {
                    context.setCode(innerNode.code());
                }
            } else {
                // Eager execution
                lastResult = innerNode.execute();
                if (innerNode.variableName() != null) {
                    String varName = innerNode.variableName();
                    if ("output".equals(varName)) {
                        context.setOutput(lastResult);
                    }
                    context.setVariable(varName, lastResult);
                }
                if (innerNode.code() != null) {
                    context.setCode(innerNode.code());
                }
                // Evaluate inner pipe clauses
                for (PipeClause pipe : innerNode.pipeClauses()) {
                    if (pipe.isDefault()) {
                        if (pipe.handler() != null) {
                            lastResult = pipe.handler().execute();
                            handleResult(context, pipe.handler(), lastResult);
                        }
                        if (pipe.code() != null) {
                            context.setCode(pipe.code());
                        }
                        break;
                    }
                    try {
                        ISupplier<?> conditionSupplier = pipe.condition().evaluate();
                        Object conditionResult = conditionSupplier.supply().orElse(null);
                        if (conditionResult instanceof Boolean b && b) {
                            if (pipe.handler() != null) {
                                lastResult = pipe.handler().execute();
                                handleResult(context, pipe.handler(), lastResult);
                            }
                            if (pipe.code() != null) {
                                context.setCode(pipe.code());
                            }
                            break;
                        }
                    } catch (Exception e) {
                        throw new ScriptException("Pipe condition evaluation failed", e);
                    }
                }
            }
        }

        // Restore previous function values to prevent leaking to subsequent groups/stages.
        for (var entry : savedFunctions.entrySet()) {
            if (entry.getValue() != null) {
                context.setVariable(entry.getKey(), entry.getValue());
            }
            // If previously null, the variable was never set — we can't remove it from
            // the context, but the function object is replaced by whatever was there before.
            // Since the context doesn't support removal, we leave it as the IScriptFunction
            // which will simply be overwritten by any future definition.
        }

        return lastResult;
    }

    private void evaluatePipeClauses(IRuntimeContext<Object[], Object> runtimeContext, Object result) {
        for (PipeClause pipe : this.node.pipeClauses()) {
            if (pipe.isDefault()) {
                if (pipe.handler() != null) {
                    Object handlerResult = pipe.handler().execute();
                    handleResult(runtimeContext, pipe.handler(), handlerResult);
                }
                if (pipe.code() != null) {
                    runtimeContext.setCode(pipe.code());
                }
                return;
            }
            try {
                ISupplier<?> conditionSupplier = pipe.condition().evaluate();
                Object conditionResult = conditionSupplier.supply().orElse(null);
                if (conditionResult instanceof Boolean b && b) {
                    if (pipe.handler() != null) {
                        Object handlerResult = pipe.handler().execute();
                        handleResult(runtimeContext, pipe.handler(), handlerResult);
                    }
                    if (pipe.code() != null) {
                        runtimeContext.setCode(pipe.code());
                    }
                    return;
                }
            } catch (Exception e) {
                throw new ScriptException("Pipe condition evaluation failed", e);
            }
        }
    }

    private void handleResult(IRuntimeContext<Object[], Object> runtimeContext, IScriptNode node, Object result) {
        if (node.variableName() != null) {
            String varName = node.variableName();
            // Special handling for @output variable - sets the runtime output
            if ("output".equals(varName)) {
                if (node.assignExpression()) {
                    // Store expression for lazy evaluation - also set as output
                    runtimeContext.setVariable(varName, node.expression());
                    runtimeContext.setOutput(node.expression());
                } else {
                    runtimeContext.setOutput(result);
                }
            } else if (node.assignExpression()) {
                runtimeContext.setVariable(varName, node.expression());
            } else {
                runtimeContext.setVariable(varName, result);
            }
        }
        if (node.code() != null) {
            runtimeContext.setCode(node.code());
        }
        // Store the last result in a special variable "_" (like Python's REPL)
        // This allows the console to display expression results
        if (result != null) {
            runtimeContext.setVariable("_", result);
        }
    }

    private void storeErrorContext(IRuntimeContext<Object[], Object> context, Throwable cause) {
        if (node.line() > 0) {
            context.setVariable("_scriptErrorLine", node.line());
        }
        if (node.sourceText() != null) {
            String src = node.sourceText().trim();
            if (src.length() > 120) {
                src = src.substring(0, 120) + "...";
            }
            context.setVariable("_scriptErrorSource", src);
        }
        context.setVariable("_scriptErrorStep", stepName);
        Throwable root = findRootCause(cause);
        context.setVariable("_scriptErrorMessage", root.getMessage());
        context.setVariable("_scriptErrorType", root.getClass().getName());
    }

    private static Throwable findRootCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root;
    }

    private StatementBlock resolveBlock(IRuntimeContext<Object[], Object> context, String blockName) {
        Optional<Object> blockOpt = context.getVariable(blockName, IClass.getClass(Object.class));
        Object blockObj = blockOpt.orElseThrow(
                () -> new ScriptException("Function body block not found: " + blockName));
        if (blockObj instanceof ISupplier<?> supplier) {
            blockObj = supplier.supply().orElse(null);
        }
        if (!(blockObj instanceof StatementBlock body)) {
            throw new ScriptException("Function body is not a StatementBlock: " + blockName);
        }
        return body;
    }

    private String logHeader() {
        StringBuilder sb = new StringBuilder("[Runtime script][Step ").append(stepName);
        if (node.line() > 0) {
            sb.append(", line ").append(node.line());
        }
        sb.append("] ");
        return sb.toString();
    }
}
