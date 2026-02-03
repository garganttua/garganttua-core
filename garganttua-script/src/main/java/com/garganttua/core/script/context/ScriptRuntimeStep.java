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
                        return context.getVariable(name, type);
                    }
                    @Override
                    public void setVariable(String name, Object value) {
                        context.setVariable(name, value);
                    }
                });
                try {
                    Object result = node.execute();
                    handleResult(context, node, result);
                    evaluatePipeClauses(context, result);
                } catch (ScriptException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    for (CatchClause catchClause : node.catchClauses()) {
                        if (catchClause.matches(cause)) {
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
                try {
                    Optional<RuntimeExceptionRecord> abortingEx = context.findAbortingExceptionReport();
                    if (abortingEx.isPresent()) {
                        Throwable cause = abortingEx.get().exception();
                        for (CatchClause cc : node.downstreamCatchClauses()) {
                            if (cc.matches(cause)) {
                                Object handlerResult = cc.handler().execute();
                                handleResult(context, cc.handler(), handlerResult);
                                return;
                            }
                        }
                    }
                } finally {
                    RuntimeExpressionContext.clear();
                }
                nextFallback.executeFallBack(context);
            };
            chain.addExecutor(executor, fallback);
        }
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
            if (node.assignExpression()) {
                runtimeContext.setVariable(node.variableName(), node.expression());
            } else {
                runtimeContext.setVariable(node.variableName(), result);
            }
        }
        if (node.code() != null) {
            runtimeContext.setCode(node.code());
        }
    }

    private String logHeader() {
        return "[Runtime script][Step " + stepName + "] ";
    }
}
