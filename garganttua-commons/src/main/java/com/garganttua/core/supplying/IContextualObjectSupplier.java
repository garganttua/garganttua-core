package com.garganttua.core.supplying;

import java.util.Optional;

public interface IContextualObjectSupplier<Supplied, Context> extends IObjectSupplier<Supplied> {

    @Override
    default Optional<Supplied> supply() throws SupplyException {
        throw new SupplyException("Owner context of type "+getOwnerContextClass().getSimpleName()+" required for this supplier");
    }

    Class<Context> getOwnerContextClass();

    Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException;

}
