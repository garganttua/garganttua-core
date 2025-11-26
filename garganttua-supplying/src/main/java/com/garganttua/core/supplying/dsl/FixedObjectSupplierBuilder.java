package com.garganttua.core.supplying.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.FixedObjectSupplier;

public class FixedObjectSupplierBuilder<Supplied>
        implements IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> {

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
    public Class<Supplied> getSuppliedType() {
        return (Class<Supplied>) this.object.getClass();
    }

    public static <Supplied> IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> of(Supplied object) {
        return new FixedObjectSupplierBuilder<>(object);
    }

    public static <Supplied> IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> ofNullable(Supplied object, Class<Supplied> type) {
        if( object != null )
            return new FixedObjectSupplierBuilder<>(object);
        
        return new NullObjectSupplierBuilder<>(type);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
