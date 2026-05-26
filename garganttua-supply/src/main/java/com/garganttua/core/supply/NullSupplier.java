package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;

public class NullSupplier<SuppliedType> implements ISupplier<SuppliedType>{
    private static final IDiagnostic log = Diagnostics.of(NullSupplier.class);

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;

    public NullSupplier(IClass<SuppliedType> suppliedClass) {
        this.suppliedType = suppliedClass.getType();
        this.suppliedClass = suppliedClass;
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.trace("Entering supply method");
        log.debug("Supplying null object for type {}", this.suppliedClass.getSimpleName());
        log.debug("Supply completed for null object of type {}", this.suppliedClass.getSimpleName());
        log.trace("Exiting supply method with empty result");
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
