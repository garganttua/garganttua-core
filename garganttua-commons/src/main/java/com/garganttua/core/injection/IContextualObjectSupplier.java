package com.garganttua.core.injection;

import java.util.Optional;

import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.SupplyException;

public interface IContextualObjectSupplier<Supplied, Context> extends IObjectSupplier<Supplied> {

    @Override
    default Optional<Supplied> getObject() throws SupplyException {
        throw new SupplyException("Context required for this supplier");
    }

    Class<Context> getContextClass();

    Optional<Supplied> getObject(Context context) throws SupplyException;

}
