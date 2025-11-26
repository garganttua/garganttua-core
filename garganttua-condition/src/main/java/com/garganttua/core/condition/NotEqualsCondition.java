package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;

public class NotEqualsCondition<T> implements ICondition {

    private IObjectSupplier<T> supplier1;
    private IObjectSupplier<T> supplier2;

    public NotEqualsCondition(IObjectSupplier<T> supplier1, IObjectSupplier<T> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");
        if (!this.supplier1.getSuppliedType().equals(this.supplier2.getSuppliedType()))
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedType().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedType().getSimpleName());
    }

    @Override
    public boolean evaluate() throws ConditionException {
        return !new EqualsCondition<>(supplier1, supplier2).evaluate();
    }

}
