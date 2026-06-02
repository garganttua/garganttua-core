package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

public class NewContextualSupplier<SuppliedType>
        implements IContextualSupplier<SuppliedType, Void> {
    private static final Logger log = Logger.getLogger(NewContextualSupplier.class);

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;
    private IClass<Void> ownerContextClass;
    private IContextualConstructorBinder<SuppliedType> constructorBinder;

    public NewContextualSupplier(IClass<SuppliedType> suppliedClass,
            IClass<Void> ownerContextClass,
            IContextualConstructorBinder<SuppliedType> constructorBinder) {
        log.trace("Entering NewContextualSupplier constructor with suppliedClass: {}", suppliedClass);
        this.constructorBinder = constructorBinder;
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.suppliedType = suppliedClass.getType();
        this.ownerContextClass = Objects.requireNonNull(ownerContextClass, "Owner context class cannot be null");
        log.trace("Exiting NewContextualSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply(Void ownerContext, Object... contexts)
            throws SupplyException {
        log.trace("Entering supply method with contexts count: {}", contexts.length);
        log.debug("Supplying new contextual object of type {} using contextual constructor binder", this.suppliedClass.getSimpleName());

        Objects.requireNonNull(ownerContext, "Owner cannot be null");

        try {
            Optional<IMethodReturn<SuppliedType>> result = this.constructorBinder.execute(ownerContext, contexts);

            if (result.isEmpty()) {
                log.warn("Supply failed for type {}: result is empty", this.suppliedClass.getSimpleName());
                throw new SupplyException("Constructor binder returned empty result for type " + this.suppliedClass.getSimpleName());
            }

            IMethodReturn<SuppliedType> methodReturn = result.get();

            if (methodReturn.hasException()) {
                Throwable exception = methodReturn.getException();
                log.warn("Supply failed for type {} due to exception: {}", this.suppliedClass.getSimpleName(), exception.getMessage());
                throw new SupplyException("Constructor threw exception for type " + this.suppliedClass.getSimpleName(), exception);
            }

            SuppliedType value = methodReturn.single();
            log.debug("Supply completed for new contextual object of type {}", this.suppliedClass.getSimpleName());
            log.trace("Exiting supply method");
            return Optional.ofNullable(value);
        } catch (ReflectionException e) {
            log.warn("Supply failed for type {} due to ReflectionException: {}", this.suppliedClass.getSimpleName(), e.getMessage());
            throw new SupplyException("Reflection error during supply of type " + this.suppliedClass.getSimpleName(), e);
        }
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public IClass<Void> getOwnerContextType() {
        return this.ownerContextClass;
    }

    @Override
    public IClass<SuppliedType> getSuppliedClass() {
        return this.suppliedClass;
    }

}
