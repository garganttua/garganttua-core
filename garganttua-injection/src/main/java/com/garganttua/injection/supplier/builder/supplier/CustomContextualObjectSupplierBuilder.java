package com.garganttua.injection.supplier.builder.supplier;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IContextualObjectSupplier;
import com.garganttua.core.injection.ICustomContextualObjectSupply;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.CustomContextualObjectSupplier;

public class CustomContextualObjectSupplierBuilder<Supplied, Context> implements IObjectSupplierBuilder<Supplied, IContextualObjectSupplier<Supplied, Context>> {

    private ICustomContextualObjectSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public CustomContextualObjectSupplierBuilder(ICustomContextualObjectSupply<Supplied, Context> supply, Class<Supplied> suppliedType, Class<Context> contextType) {
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
    }

    @Override
    public Class<Supplied> getObjectClass() {
        return this.suppliedType;
    }

    @Override
    public IContextualObjectSupplier<Supplied, Context> build() throws DslException {
        return new CustomContextualObjectSupplier<>(this.supply, this.suppliedType, this.contextType);
    }

}
