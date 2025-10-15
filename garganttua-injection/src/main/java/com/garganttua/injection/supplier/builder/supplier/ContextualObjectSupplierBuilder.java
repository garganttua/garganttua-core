package com.garganttua.injection.supplier.builder.supplier;

import java.util.Objects;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.supplier.IContextualObjectSupplier;
import com.garganttua.injection.spec.supplier.IContextualObjectSupply;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.ContextualObjectSupplier;

public class ContextualObjectSupplierBuilder<Supplied> implements IObjectSupplierBuilder<Supplied, IContextualObjectSupplier<Supplied, IDiContext>> {

    private IContextualObjectSupply<Supplied> supply;
    private Class<Supplied> suppliedType;

    public ContextualObjectSupplierBuilder(IContextualObjectSupply<Supplied> supply, Class<Supplied> suppliedType) {
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
    }

    @Override
    public IContextualObjectSupplier<Supplied, IDiContext> build() throws DslException {
        return new ContextualObjectSupplier<>(this.supply, this.suppliedType);
    }

    @Override
    public Class<Supplied> getObjectClass() {
        return this.suppliedType;
    }

}
