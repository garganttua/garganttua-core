package com.garganttua.core.script.nodes;

import com.garganttua.core.expression.IExpression;
import com.garganttua.core.supply.ISupplier;

public class PipeClause {

    private final IExpression<?, ? extends ISupplier<?>> condition;
    private final IScriptNode handler;

    public PipeClause(IExpression<?, ? extends ISupplier<?>> condition, IScriptNode handler) {
        this.condition = condition;
        this.handler = handler;
    }

    public IExpression<?, ? extends ISupplier<?>> condition() {
        return this.condition;
    }

    public IScriptNode handler() {
        return this.handler;
    }

    public boolean isDefault() {
        return this.condition == null;
    }
}
