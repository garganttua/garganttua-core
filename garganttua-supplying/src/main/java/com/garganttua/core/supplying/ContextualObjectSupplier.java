package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupply;
import com.garganttua.core.supply.SupplyException;

public class ContextualObjectSupplier<Supplied, Context> implements IContextualObjectSupplier<Supplied, Context> {

    private IContextualObjectSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public ContextualObjectSupplier(IContextualObjectSupply<Supplied, Context> supply,
            Class<Supplied> suppliedType, Class<Context> contextType) {
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
    }

    @Override
    public Class<Supplied> getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public Class<Context> getOwnerContextType() {
        return this.contextType;
    }

    @Override
    public Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException {
        if (!this.contextType.isAssignableFrom(ownerContext.getClass())) {
            throw new SupplyException("Context type mismatch : waiting " + this.contextType.getSimpleName() + " but "
                    + ownerContext.getClass().getSimpleName() + " provided");
        }
        return this.supply.supplyObject(ownerContext, otherContexts);
    }

}
