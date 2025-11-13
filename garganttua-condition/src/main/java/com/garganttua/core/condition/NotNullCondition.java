package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.supplying.IObjectSupplier;

public class NotNullCondition implements ICondition {

    private IObjectSupplier<?> supplier;

    public NotNullCondition(IObjectSupplier<?> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        return !new NullCondition(supplier).evaluate();
    }

}
