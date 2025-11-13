package com.garganttua.core.condition.dsl;

import java.util.Objects;

import com.garganttua.core.condition.ICondition;
import com.garganttua.core.condition.NullCondition;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class NullConditionBuilder<T> implements IConditionBuilder {

    private IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier;

    public NullConditionBuilder(IObjectSupplierBuilder<T, IObjectSupplier<T>> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "Object supplier builder cannot be null");
    }

    @Override
    public ICondition build() throws DslException {
        return new NullCondition(this.supplier.build());
    }

}
