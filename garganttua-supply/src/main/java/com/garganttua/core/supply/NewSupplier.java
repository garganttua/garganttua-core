package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewSupplier<SuppliedType> implements ISupplier<SuppliedType> {

    private Class<SuppliedType> suppliedType;
    private IConstructorBinder<SuppliedType> constructorBinder;

    public NewSupplier(Class<SuppliedType> suppliedType,
            IConstructorBinder<SuppliedType> constructorBinder) {
        log.atTrace().log("Entering NewSupplier constructor with suppliedType: {}", suppliedType);
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atTrace().log("Exiting NewSupplier constructor");
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying new object of type {} using constructor binder", this.suppliedType.getSimpleName());

        try {
            Optional<SuppliedType> result = (Optional<SuppliedType>) this.constructorBinder.execute();
            log.atDebug().log("Supply completed for new object of type {}, result present: {}", this.suppliedType.getSimpleName(), result.isPresent());
            log.atTrace().log("Exiting supply method");
            return result;
        } catch (ReflectionException e) {
            log.atWarn().log("Supply failed for type {} due to ReflectionException: {}", this.suppliedType.getSimpleName(), e.getMessage());
            log.atTrace().log("Exiting supply method with empty result");
            return Optional.empty();
        }
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

}