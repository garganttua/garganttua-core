package com.garganttua.core.condition;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.garganttua.core.supply.IObjectSupplier;

public class CustomCondition<T> implements ICondition  {

    private IObjectSupplier<T> supplier;
    private Predicate<T> predicate;

    public CustomCondition(IObjectSupplier<T> supplier,
            Predicate<T> predicate) {
        this.supplier = Objects.requireNonNull(supplier, "Supplier cannot be null");
        this.predicate = Objects.requireNonNull(predicate, "Predicate cannot be null");
    }

    @Override
    public boolean evaluate() throws ConditionException {
        Optional<T> value = supplier.supply();
        if (value.isEmpty()) throw new ConditionException("Supplied value is empty or null"); 
        return predicate.test(value.get());
    }
}
