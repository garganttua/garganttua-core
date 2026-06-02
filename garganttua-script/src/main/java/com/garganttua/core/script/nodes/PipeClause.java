package com.garganttua.core.script.nodes;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

/**
 * A conditional pipe clause ({@code | condition => handler}) in the script AST.
 * When its condition evaluates truthy, the handler node runs and an optional exit
 * code is applied. A {@code null} condition marks the default (always-matching) branch.
 */
public class PipeClause {

    private final IExpression<?, ? extends ISupplier<?>> condition;
    private final IScriptNode handler;
    private final Integer code;

    /**
     * Creates a pipe clause with no associated exit code.
     *
     * @param condition the guard expression, or {@code null} for the default branch
     * @param handler   the node to execute when the condition holds
     */
    public PipeClause(IExpression<?, ? extends ISupplier<?>> condition, IScriptNode handler) {
        this(condition, handler, null);
    }

    /**
     * Creates a pipe clause.
     *
     * @param condition the guard expression, or {@code null} for the default branch
     * @param handler   the node to execute when the condition holds
     * @param code      the exit code to apply when matched, or {@code null}
     */
    public PipeClause(IExpression<?, ? extends ISupplier<?>> condition, IScriptNode handler, Integer code) {
        this.condition = condition;
        this.handler = handler;
        this.code = code;
    }

    /** Returns the guard expression, or {@code null} for the default branch. */
    public IExpression<?, ? extends ISupplier<?>> condition() {
        return this.condition;
    }

    /** Returns the handler node executed when the condition holds. */
    public IScriptNode handler() {
        return this.handler;
    }

    /** Returns the exit code applied when matched, or {@code null}. */
    public Integer code() {
        return this.code;
    }

    /** Returns {@code true} if this is the default (unconditional) branch. */
    public boolean isDefault() {
        return this.condition == null;
    }
}
