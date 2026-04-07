package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.expression.context.ExpressionVariableContext;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.RuntimeExpressionContext;
import com.garganttua.core.script.ScriptException;
import com.garganttua.core.script.context.ScriptVariableResolver;
import com.garganttua.core.supply.ISupplier;

/**
 * An ordered list of script statements that can be executed as a unit.
 *
 * <p>Statement blocks are used in two contexts:
 * <ul>
 *   <li>As function bodies in user-defined functions ({@link ScriptFunction})</li>
 *   <li>As lazy arguments to control flow functions like {@code if(condition, block)}
 *       — the block is passed as an object and only executed when the condition matches
 *       (see {@link com.garganttua.core.script.functions.ControlFlowFunctions})</li>
 * </ul>
 *
 * <p>Blocks are extracted from the script source by
 * {@link com.garganttua.core.script.context.BlockExpressionPreprocessor} before
 * ANTLR parsing, compiled into {@code StatementBlock} instances, and stored as
 * variables in the runtime context.
 */
public class StatementBlock {

    private final List<IScriptNode> statements;

    public StatementBlock(List<IScriptNode> statements) {
        this.statements = statements != null ? List.copyOf(statements) : List.of();
    }

    public List<IScriptNode> statements() {
        return this.statements;
    }

    private static final ScriptVariableResolver RESOLVER = new ScriptVariableResolver();

    @SuppressWarnings("unchecked")
    public Object execute() {
        IRuntimeContext<Object[], Object> context = RuntimeExpressionContext.get();
        if (context == null) {
            throw new ScriptException("StatementBlock: no runtime context available");
        }
        // Ensure ExpressionVariableContext is set so inner expressions can
        // resolve script variables (@0, @code, @output, named variables).
        // This is needed because StatementBlock is executed by ControlFlowFunctions.ifExpr()
        // and ScriptFunction, which are outside the ScriptExpressionWrapper scope.
        var previous = ExpressionVariableContext.get();
        ExpressionVariableContext.set(RESOLVER);
        try {
            return executeStatements(context, this.statements);
        } finally {
            if (previous != null) {
                ExpressionVariableContext.set(previous);
            } else {
                ExpressionVariableContext.clear();
            }
        }
    }

    static Object executeStatements(IRuntimeContext<Object[], Object> context, List<IScriptNode> statements) {
        Object lastResult = null;
        for (IScriptNode node : statements) {
            // Ensure the resolver is set before each statement — previous statements
            // (e.g. execute_script) may have cleared it via sub-script execution
            ExpressionVariableContext.set(RESOLVER);
            try {
                if (node instanceof StatementGroupNode group) {
                    lastResult = executeStatements(context, group.statements());
                    setVar(context, group, lastResult);
                } else if (node.assignExpression() && node.variableName() != null) {
                    ISupplier<?> supplier = node.expression().evaluate();
                    context.setVariable(node.variableName(), supplier);
                    if (node.code() != null) {
                        context.setCode(node.code());
                    }
                } else {
                    lastResult = node.execute();
                    setVar(context, node, lastResult);
                }
            } catch (ScriptException e) {
                var cr = CatchClauseHandler.tryCatchClauses(context, node.catchClauses(), e);
                if (cr.caught()) {
                    lastResult = cr.handlerResult();
                    break;
                }
                throw e;
            }
        }
        return lastResult;
    }

    private static void setVar(IRuntimeContext<Object[], Object> context, IScriptNode node, Object result) {
        if (node.variableName() != null) {
            String name = node.variableName();
            if ("output".equals(name) && result != null) {
                context.setOutput(result);
            }
            if (result != null) {
                context.setVariable(name, result);
            }
        }
        if (node.code() != null) {
            context.setCode(node.code());
        }
        if (result != null) {
            context.setVariable("_", result);
        }
    }
}
