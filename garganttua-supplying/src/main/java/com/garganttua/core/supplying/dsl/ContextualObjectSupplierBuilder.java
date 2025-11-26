package com.garganttua.core.supplying.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupply;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.ContextualObjectSupplier;

public class ContextualObjectSupplierBuilder<Supplied, Context> implements IObjectSupplierBuilder<Supplied, IContextualObjectSupplier<Supplied, Context>> {

    private IContextualObjectSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public ContextualObjectSupplierBuilder(IContextualObjectSupply<Supplied, Context> supply, Class<Supplied> suppliedType, Class<Context> contextType) {
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
    }

    @Override
    public Class<Supplied> getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public IContextualObjectSupplier<Supplied, Context> build() throws DslException {
        return new ContextualObjectSupplier<>(this.supply, this.suppliedType, this.contextType);
    }

    @Override
    public boolean isContextual() {
        return true;
    }

}
