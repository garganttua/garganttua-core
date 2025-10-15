package com.garganttua.injection.spec.supplier;

import java.util.Optional;

import com.garganttua.injection.DiException;

public interface IContextualObjectSupplier<Supplied, Context> extends IObjectSupplier<Supplied> {

    @Override
    default Optional<Supplied> getObject() throws DiException {
        throw new DiException("Context required for this supplier");
    }

    Class<Context> getContextClass();

    Optional<Supplied> getObject(Context context) throws DiException;

}
