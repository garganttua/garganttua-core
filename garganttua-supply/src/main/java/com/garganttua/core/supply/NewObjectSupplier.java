package com.garganttua.core.supply;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType> {

    private Class<SuppliedType> suppliedType;
    private IConstructorBinder<SuppliedType> constructorBinder;

    public NewObjectSupplier(Class<SuppliedType> suppliedType,
            IConstructorBinder<SuppliedType> constructorBinder) {
        log.atTrace().log("Entering NewObjectSupplier constructor with suppliedType: {}", suppliedType);
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atTrace().log("Exiting NewObjectSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying new object of type {} using constructor binder", this.suppliedType.getSimpleName());

        try {
            Optional<SuppliedType> result = this.constructorBinder.execute();
            log.atInfo().log("Supply completed for new object of type {}, result present: {}", this.suppliedType.getSimpleName(), result.isPresent());
            log.atTrace().log("Exiting supply method");
            return result;
        } catch (ReflectionException e) {
            log.atWarn().log("Supply failed for type {} due to ReflectionException: {}", this.suppliedType.getSimpleName(), e.getMessage());
            log.atTrace().log("Exiting supply method with empty result");
            return Optional.empty();
        }
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.suppliedType;
    }

}