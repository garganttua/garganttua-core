package com.garganttua.core.condition;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;

public class EqualsCondition<T> implements ICondition {

    private IObjectSupplier<T> supplier1;
    private IObjectSupplier<T> supplier2;

    public EqualsCondition(IObjectSupplier<T> supplier1, IObjectSupplier<T> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier 2 cannot be null");
        if (!this.supplier1.getSuppliedType().equals(this.supplier2.getSuppliedType()))
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedType().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedType().getSimpleName());
    }

    @Override
    public boolean evaluate() throws ConditionException {
        this.supplier1.supply().orElseThrow(() -> new ConditionException("Supplier 1 supplied empty value"));
        this.supplier2.supply().orElseThrow(() -> new ConditionException("Supplier 2 supplied empty value"));

        return this.supplier1.supply().get().equals(this.supplier2.supply().get());
    }

}
