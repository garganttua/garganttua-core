package com.garganttua.core.supply;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedObjectSupplier<Supplied> implements IObjectSupplier<Supplied> {

    private Supplied object;

    public FixedObjectSupplier(Supplied object) {
        log.atTrace().log("Entering FixedObjectSupplier constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        log.atTrace().log("Exiting FixedObjectSupplier constructor");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying fixed object of type {}", this.object.getClass().getSimpleName());
        Optional<Supplied> result = Optional.of(this.object);
        log.atInfo().log("Supply completed for fixed object of type {}", this.object.getClass().getSimpleName());
        log.atTrace().log("Exiting supply method");
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Supplied> getSuppliedType() {
        return (Class<Supplied>) this.object.getClass();
    }

}
