package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedSupplier<Supplied> implements ISupplier<Supplied> {

    private Supplied object;
    private IClass<Supplied> suppliedClass;

    public FixedSupplier(Supplied object, IClass<Supplied> suppliedClass) {
        log.atTrace().log("Entering FixedSupplier constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.atTrace().log("Exiting FixedSupplier constructor");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying fixed object of type {}", this.object.getClass().getSimpleName());
        Optional<Supplied> result = Optional.of(this.object);
        log.atDebug().log("Supply completed for fixed object of type {}", this.object.getClass().getSimpleName());
        log.atTrace().log("Exiting supply method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

}
