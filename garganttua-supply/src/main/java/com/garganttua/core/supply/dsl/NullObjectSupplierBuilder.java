package com.garganttua.core.supply.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.NullObjectSupplier;

public class NullObjectSupplierBuilder<SuppliedType>
        implements IObjectSupplierBuilder<SuppliedType, IObjectSupplier<SuppliedType>> {

    private Class<SuppliedType> class1;

    public NullObjectSupplierBuilder(Class<SuppliedType> class1) {
        this.class1 = class1;
    }

    @Override
    public IObjectSupplier<SuppliedType> build() throws DslException {
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

    public static <SuppliedType> NullObjectSupplierBuilder<SuppliedType> of(Class<SuppliedType> class1){
        return new NullObjectSupplierBuilder<>(class1);
    }

}
