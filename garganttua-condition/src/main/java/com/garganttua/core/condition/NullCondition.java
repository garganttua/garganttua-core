package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.supply.IObjectSupplier;

public class NullCondition implements ICondition {

    private IObjectSupplier<?> supplier;

    public NullCondition(IObjectSupplier<?> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        try {
            Optional<?> supplied = this.supplier.supply();
            if (supplied.isPresent())
                return false;

        } catch (Exception e) {
            return true;
        }
        return true;
    }

}
