package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

public class FixedObjectSupplier<Supplied> implements IObjectSupplier<Supplied> {

    private Supplied object;

    public FixedObjectSupplier(Supplied object) {
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        return Optional.of(this.object);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Supplied> getSuppliedType() {
        return (Class<Supplied>) this.object.getClass();
    }

}
