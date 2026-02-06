package com.garganttua.core.script.context;

import java.util.Optional;

import com.garganttua.core.execution.IExecutorChain;
import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.expression.ForLoopExpressionNode;
import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.expression.context.IExpressionVariableResolver;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeExceptionRecord;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.runtime.RuntimeStepExecutionTools;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.nodes.CatchClause;
import com.garganttua.core.script.nodes.IScriptNode;
import com.garganttua.core.script.nodes.PipeClause;
import com.garganttua.core.script.nodes.StatementGroupNode;
import com.garganttua.core.supply.ISupplier;

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
                    @Override
                    public <T> java.util.Optional<T> resolve(String name, Class<T> type) {
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
                                    if (type == Object.class) {
                                        @SuppressWarnings("unchecked")
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
                            if (code.isPresent() && type.isAssignableFrom(Integer.class)) {
                                @SuppressWarnings("unchecked")
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
                            if (output != null && type == Object.class) {
                                @SuppressWarnings("unchecked")
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
                            @SuppressWarnings("unchecked")
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
                            Object handlerResult = catchClause.handler().execute();
                            handleResult(context, catchClause.handler(), handlerResult);
                            caught = true;
                            break;
                        }
                    }
                    if (!caught) {
                        RuntimeStepExecutionTools.handleException(
                                "script", stepName, context, cause, true,
                                "script:" + stepName, null, logHeader());
                    }
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
                    @Override
                    public <T> java.util.Optional<T> resolve(String name, Class<T> type) {
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
                                    if (type == Object.class) {
                                        @SuppressWarnings("unchecked")
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
                            if (code.isPresent() && type.isAssignableFrom(Integer.class)) {
                                @SuppressWarnings("unchecked")
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
                            if (output != null && type == Object.class) {
                                @SuppressWarnings("unchecked")
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
                            @SuppressWarnings("unchecked")
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
                                Object handlerResult = cc.handler().execute();
                                handleResult(context, cc.handler(), handlerResult);
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
    private Object executeGroup(IRuntimeContext<Object[], Object> context, StatementGroupNode groupNode) throws ScriptException {
        Object lastResult = null;
        for (IScriptNode innerNode : groupNode.statements()) {
            if (innerNode instanceof StatementGroupNode innerGroup) {
                // Recursively execute nested groups
                lastResult = executeGroup(context, innerGroup);
                // Handle nested group's variable assignment
                if (innerGroup.variableName() != null) {
                    context.setVariable(innerGroup.variableName(), lastResult);
                }
                if (innerGroup.code() != null) {
                    context.setCode(innerGroup.code());
                }
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
                    context.setVariable(innerNode.variableName(), lastResult);
                }
                if (innerNode.code() != null) {
                    context.setCode(innerNode.code());
                }
                // Evaluate inner pipe clauses
                for (PipeClause pipe : innerNode.pipeClauses()) {
                    if (pipe.isDefault()) {
                        lastResult = pipe.handler().execute();
                        handleResult(context, pipe.handler(), lastResult);
                        break;
                    }
                    try {
                        ISupplier<?> conditionSupplier = pipe.condition().evaluate();
                        Object conditionResult = conditionSupplier.supply().orElse(null);
                        if (conditionResult instanceof Boolean b && b) {
                            lastResult = pipe.handler().execute();
                            handleResult(context, pipe.handler(), lastResult);
                            break;
                        }
                    } catch (Exception e) {
                        throw new ScriptException("Pipe condition evaluation failed", e);
                    }
                }
            }
        }
        return lastResult;
    }

    private void evaluatePipeClauses(IRuntimeContext<Object[], Object> runtimeContext, Object result) {
        for (PipeClause pipe : this.node.pipeClauses()) {
            if (pipe.isDefault()) {
                Object handlerResult = pipe.handler().execute();
                handleResult(runtimeContext, pipe.handler(), handlerResult);
                return;
            }
            try {
                ISupplier<?> conditionSupplier = pipe.condition().evaluate();
                Object conditionResult = conditionSupplier.supply().orElse(null);
                if (conditionResult instanceof Boolean b && b) {
                    Object handlerResult = pipe.handler().execute();
                    handleResult(runtimeContext, pipe.handler(), handlerResult);
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

    private String logHeader() {
        return "[Runtime script][Step " + stepName + "] ";
    }
}
