package com.garganttua.core.script.nodes;

import java.util.List;

import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.script.ScriptException;

/**
 * Utility for matching catch clauses against exceptions and executing handlers.
 * Used by {@link com.garganttua.core.script.context.ScriptRuntimeStep} and
 * {@link StatementBlock} to process catch clauses in nested execution contexts
 * (statement groups, function bodies, if-blocks).
 */
public class CatchClauseHandler {

    public record CatchResult(boolean caught, Object handlerResult) {}

    /**
     * Attempts to match the given exception against a list of catch clauses.
     * If a match is found, sets exception-related context variables, executes the
     * handler (if any), and applies the catch clause's code.
     *
     * @param context      the runtime context for variable and code manipulation
     * @param catchClauses the catch clauses to match against
     * @param exception    the exception to match
     * @return a {@link CatchResult} indicating whether the exception was caught
     */
    @SuppressWarnings("unchecked")
    public static CatchResult tryCatchClauses(
            IRuntimeContext<Object[], Object> context,
            List<CatchClause> catchClauses,
            ScriptException exception) {

        if (catchClauses == null || catchClauses.isEmpty()) {
            return new CatchResult(false, null);
        }

        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;

        for (CatchClause cc : catchClauses) {
            if (cc.matches(cause)) {
                context.setVariable("exception", cause);
                context.setVariable("message", cause.getMessage() != null ? cause.getMessage() : "");
                Integer currentCode = context.getCode().orElse(0);
                context.setVariable("code", currentCode);

                Object handlerResult = null;
                if (cc.handler() != null) {
                    handlerResult = cc.handler().execute();
                    applyNodeResult(context, cc.handler(), handlerResult);
                }
                if (cc.code() != null) {
                    context.setCode(cc.code());
                }
                return new CatchResult(true, handlerResult);
            }
        }
        return new CatchResult(false, null);
    }

    /**
     * Applies the result of a handler node execution to the runtime context:
     * sets the variable (if named), sets the code (if specified), and updates
     * the special {@code _} (last result) variable.
     */
    static void applyNodeResult(IRuntimeContext<Object[], Object> context, IScriptNode node, Object result) {
        if (node.variableName() != null) {
            String name = node.variableName();
            if ("output".equals(name)) {
                context.setOutput(result);
            }
            context.setVariable(name, result);
        }
        if (node.code() != null) {
            context.setCode(node.code());
        }
        if (result != null) {
            context.setVariable("_", result);
        }
    }
}
