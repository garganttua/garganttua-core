package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.condition.EqualsCondition;
import com.garganttua.core.condition.ICondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class EqualsConditionBuilder<T> implements IConditionBuilder {

    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1;
    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2;

    public EqualsConditionBuilder(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier1,
            IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier2) {
        this.supplier1 = Objects.requireNonNull(supplier1, "Object supplier builder 1 cannot be null");
        this.supplier2 = Objects.requireNonNull(supplier2, "Object supplier builder 2 cannot be null");
        if (!this.supplier1.getSuppliedType().equals(this.supplier2.getSuppliedType()))
            throw new DslException("Type mismatch " + this.supplier1.getSuppliedType().getSimpleName() + " VS "
                    + this.supplier2.getSuppliedType().getSimpleName());
    }

    @Override
    public ICondition build() throws DslException {
        return new EqualsCondition<>(supplier1.build(), supplier2.build());
    }

}
