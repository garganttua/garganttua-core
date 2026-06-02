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

    /**
     * Creates a statement block.
     *
     * @param statements the ordered statements, or {@code null} for an empty block
     */
    public StatementBlock(List<IScriptNode> statements) {
        this.statements = statements != null ? List.copyOf(statements) : List.of();
    }

    /** Returns the ordered statements in this block. */
    public List<IScriptNode> statements() {
        return this.statements;
    }

    private static final ScriptVariableResolver RESOLVER = new ScriptVariableResolver();

    /**
     * Executes the statements in order within a bound expression-variable scope and
     * returns the result of the last statement.
     *
     * @return the last statement's result, or {@code null} if the block is empty
     * @throws ScriptException if no runtime context is bound or a statement fails uncaught
     */
    @SuppressWarnings("unchecked")
    public Object execute() {
        IRuntimeContext<Object[], Object> context = RuntimeExpressionContext.get();
        if (context == null) {
            throw new ScriptException("StatementBlock: no runtime context available");
        }
        // ScopedValue: ExpressionVariableContext is bound for the duration of
        // the lambda. Inner sub-scripts that re-bind it (via ScriptExpressionWrapper)
        // create nested scopes whose values are auto-discarded on return, so the
        // outer binding remains valid across the whole statement list.
        return ExpressionVariableContext.callIn(RESOLVER, () -> executeStatements(context, this.statements));
    }

    static Object executeStatements(IRuntimeContext<Object[], Object> context, List<IScriptNode> statements) {
        Object lastResult = null;
        for (IScriptNode node : statements) {
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
