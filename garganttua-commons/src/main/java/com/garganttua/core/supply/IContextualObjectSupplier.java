package com.garganttua.core.supply;

import java.util.Optional;

public interface IContextualObjectSupplier<Supplied, Context> extends IObjectSupplier<Supplied> {

    @Override
    default Optional<Supplied> supply() throws SupplyException {
        throw new SupplyException("Owner context of type "+getOwnerContextType().getSimpleName()+" required for this supplier");
    }

    Class<Context> getOwnerContextType();

    Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException;

}
