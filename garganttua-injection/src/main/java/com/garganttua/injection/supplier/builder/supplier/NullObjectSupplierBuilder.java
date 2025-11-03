package com.garganttua.injection.supplier.builder.supplier;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.NullObjectSupplier;

public class NullObjectSupplierBuilder<T> implements IObjectSupplierBuilder<T, NullObjectSupplier<T>>{

    private Class<T> class1;

    public NullObjectSupplierBuilder(Class<T> class1) {
        this.class1 = class1;
    }

    @Override
    public NullObjectSupplier<T> build() throws DslException {
        return new NullObjectSupplier<>(this.class1);
    }

    @Override
    public Class<T> getObjectClass() {
        return this.class1;
    }

}
