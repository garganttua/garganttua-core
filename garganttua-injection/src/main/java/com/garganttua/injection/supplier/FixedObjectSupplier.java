package com.garganttua.injection.supplier;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.supplying.IObjectSupplier;

public class FixedObjectSupplier<Supplied> implements IObjectSupplier<Supplied> {

    private Supplied object;

    public FixedObjectSupplier(Supplied object) {
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
    }

    @Override
    public Optional<Supplied> getObject() throws DiException {
        return Optional.of(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Supplied> getObjectClass() {
        return (Class<Supplied>) this.object.getClass();
    }

}
