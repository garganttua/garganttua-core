package com.garganttua.injection.supplier;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IContextualObjectSupply;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.supplying.IContextualObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

public class ContextualObjectSupplier<Supplied> implements IContextualObjectSupplier<Supplied, IDiContext> {

    private IContextualObjectSupply<Supplied> supply;
    private Class<Supplied> suppliedType;

    public ContextualObjectSupplier(IContextualObjectSupply<Supplied> supply, Class<Supplied> suppliedType) {
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
    }

    @Override
    public Optional<Supplied> getObject(IDiContext context) throws SupplyException {
        Objects.requireNonNull(context, "Context cannot be null");
        return this.supply.supplyObject(context);
    }

    @Override
    public Class<Supplied> getObjectClass() {
        return this.suppliedType;
    }

    @Override
    public Class<IDiContext> getContextClass() {
        return IDiContext.class;
    }

}
