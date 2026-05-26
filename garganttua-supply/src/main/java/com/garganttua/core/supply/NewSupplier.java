package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;

public class NewSupplier<SuppliedType> implements ISupplier<SuppliedType> {
    private static final IDiagnostic log = Diagnostics.of(NewSupplier.class);

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;
    private IConstructorBinder<SuppliedType> constructorBinder;

    public NewSupplier(IClass<SuppliedType> suppliedClass,
            IConstructorBinder<SuppliedType> constructorBinder) {
        log.trace("Entering NewSupplier constructor with suppliedClass: {}", suppliedClass);
        this.constructorBinder = constructorBinder;
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.suppliedType = suppliedClass.getType();
        log.trace("Exiting NewSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.trace("Entering supply method");
        log.debug("Supplying new object of type {} using constructor binder", this.suppliedClass.getSimpleName());

        try {
            Optional<SuppliedType> result = (Optional<SuppliedType>) this.constructorBinder.execute();
            log.debug("Supply completed for new object of type {}, result present: {}", this.suppliedClass.getSimpleName(), result.isPresent());
            log.trace("Exiting supply method");
            return result;
        } catch (ReflectionException e) {
            log.warn("Supply failed for type {} due to ReflectionException: {}", this.suppliedClass.getSimpleName(), e.getMessage());
            log.trace("Exiting supply method with empty result");
            return Optional.empty();
        }
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