package com.garganttua.core.supply;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewContextualObjectSupplier<SuppliedType>
        implements IContextualObjectSupplier<SuppliedType, Void> {

    private Class<SuppliedType> suppliedType;
    private IContextualConstructorBinder<SuppliedType> constructorBinder;

    public NewContextualObjectSupplier(Class<SuppliedType> suppliedType,
            IContextualConstructorBinder<SuppliedType> constructorBinder) {
        log.atTrace().log("Entering NewContextualObjectSupplier constructor with suppliedType: {}", suppliedType);
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atTrace().log("Exiting NewContextualObjectSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply(Void ownerContext, Object... contexts)
            throws SupplyException {
        log.atTrace().log("Entering supply method with contexts count: {}", contexts.length);
        log.atDebug().log("Supplying new contextual object of type {} using contextual constructor binder", this.suppliedType.getSimpleName());

        Objects.requireNonNull(ownerContext, "Owner cannot be null");

        try {
            Optional<SuppliedType> result = this.constructorBinder.execute(ownerContext, contexts);
            log.atInfo().log("Supply completed for new contextual object of type {}, result present: {}", this.suppliedType.getSimpleName(), result.isPresent());
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

    @Override
    public Class<Void> getOwnerContextType() {
        return Void.class;
    }


}
