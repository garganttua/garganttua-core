package com.garganttua.core.supplying.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.NullObjectSupplier;

public class NullObjectSupplierBuilder<SuppliedType>
        implements IObjectSupplierBuilder<SuppliedType, NullObjectSupplier<SuppliedType>> {

    private Class<SuppliedType> class1;

    public NullObjectSupplierBuilder(Class<SuppliedType> class1) {
        this.class1 = class1;
    }

    @Override
    public NullObjectSupplier<SuppliedType> build() throws DslException {
        return new NullObjectSupplier<>(this.class1);
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.class1;
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
