package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NotNullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class NotNullConditionBuilder<T> implements IConditionBuilder {

    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier;

    public NotNullConditionBuilder(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
    }

    @Override
    public ICondition build() throws DslException {
        return new NotNullCondition(this.supplier.build());
    }

}
