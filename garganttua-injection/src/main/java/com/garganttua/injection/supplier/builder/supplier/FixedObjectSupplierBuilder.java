package com.garganttua.injection.supplier.builder.supplier;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.FixedObjectSupplier;

public class FixedObjectSupplierBuilder<Supplied> implements IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>>{

    private Supplied object;

    public FixedObjectSupplierBuilder(Supplied object) {
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
    }

    @Override
    public IObjectSupplier<Supplied> build() throws DslException {
        return new FixedObjectSupplier<>(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Supplied> getObjectClass() {
        return (Class<Supplied>) this.object.getClass();
    }

    public static <Supplied> IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> of(Supplied object) {
        return new FixedObjectSupplierBuilder<>(object);
    }

}
