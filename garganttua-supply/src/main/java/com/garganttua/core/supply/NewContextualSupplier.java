package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewContextualSupplier<SuppliedType>
        implements IContextualSupplier<SuppliedType, Void> {

    private Class<SuppliedType> suppliedType;
    private IContextualConstructorBinder<SuppliedType> constructorBinder;

    public NewContextualSupplier(Class<SuppliedType> suppliedType,
            IContextualConstructorBinder<SuppliedType> constructorBinder) {
        log.atTrace().log("Entering NewContextualSupplier constructor with suppliedType: {}", suppliedType);
        this.constructorBinder = constructorBinder;
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        log.atTrace().log("Exiting NewContextualSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply(Void ownerContext, Object... contexts)
            throws SupplyException {
        log.atTrace().log("Entering supply method with contexts count: {}", contexts.length);
        log.atDebug().log("Supplying new contextual object of type {} using contextual constructor binder", this.suppliedType.getSimpleName());

        Objects.requireNonNull(ownerContext, "Owner cannot be null");

        try {
            Optional<IMethodReturn<SuppliedType>> result = this.constructorBinder.execute(ownerContext, contexts);

            if (result.isEmpty()) {
                log.atWarn().log("Supply failed for type {}: result is empty", this.suppliedType.getSimpleName());
                throw new SupplyException("Constructor binder returned empty result for type " + this.suppliedType.getSimpleName());
            }

            IMethodReturn<SuppliedType> methodReturn = result.get();

            if (methodReturn.hasException()) {
                Throwable exception = methodReturn.getException();
                log.atWarn().log("Supply failed for type {} due to exception: {}", this.suppliedType.getSimpleName(), exception.getMessage());
                throw new SupplyException("Constructor threw exception for type " + this.suppliedType.getSimpleName(), exception);
            }

            SuppliedType value = methodReturn.single();
            log.atInfo().log("Supply completed for new contextual object of type {}", this.suppliedType.getSimpleName());
            log.atTrace().log("Exiting supply method");
            return Optional.ofNullable(value);
        } catch (ReflectionException e) {
            log.atWarn().log("Supply failed for type {} due to ReflectionException: {}", this.suppliedType.getSimpleName(), e.getMessage());
            throw new SupplyException("Reflection error during supply of type " + this.suppliedType.getSimpleName(), e);
        }
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public Class<Void> getOwnerContextType() {
        return Void.class;
    }

}
