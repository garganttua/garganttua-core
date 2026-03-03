package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Optional;

import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullSupplier<SuppliedType> implements ISupplier<SuppliedType>{

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;

    public NullSupplier(IClass<SuppliedType> suppliedClass) {
        this.suppliedType = suppliedClass.getType();
        this.suppliedClass = suppliedClass;
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying null object for type {}", this.suppliedClass.getSimpleName());
        log.atDebug().log("Supply completed for null object of type {}", this.suppliedClass.getSimpleName());
        log.atTrace().log("Exiting supply method with empty result");
        return Optional.empty();
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public IClass<SuppliedType> getSuppliedClass() {
        return this.suppliedClass;
    }

}
