package com.garganttua.core.script.nodes;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

public class PipeClause {

    private final IExpression<?, ? extends ISupplier<?>> condition;
    private final IScriptNode handler;
    private final Integer code;

    public PipeClause(IExpression<?, ? extends ISupplier<?>> condition, IScriptNode handler) {
        this(condition, handler, null);
    }

    public PipeClause(IExpression<?, ? extends ISupplier<?>> condition, IScriptNode handler, Integer code) {
        this.condition = condition;
        this.handler = handler;
        this.code = code;
    }

    public IExpression<?, ? extends ISupplier<?>> condition() {
        return this.condition;
    }

    public IScriptNode handler() {
        return this.handler;
    }

    public Integer code() {
        return this.code;
    }

    public boolean isDefault() {
        return this.condition == null;
    }
}
