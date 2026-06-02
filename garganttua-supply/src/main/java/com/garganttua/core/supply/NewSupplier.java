package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IConstructorBinder;

/**
 * Supplier that creates a new instance via a constructor binder on each call.
 *
 * @param <SuppliedType> the type of object this supplier instantiates
 * @see ISupplier
 * @see IConstructorBinder
 */
public class NewSupplier<SuppliedType> implements ISupplier<SuppliedType> {
    private static final Logger log = Logger.getLogger(NewSupplier.class);

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;
    private IConstructorBinder<SuppliedType> constructorBinder;

    /**
     * Creates an instantiating supplier.
     *
     * @param suppliedClass     the {@link IClass} of the object to instantiate
     * @param constructorBinder the constructor binder used to build instances
     */
    public NewSupplier(IClass<SuppliedType> suppliedClass,
            IConstructorBinder<SuppliedType> constructorBinder) {
        log.trace("Entering NewSupplier constructor with suppliedClass: {}", suppliedClass);
        this.constructorBinder = constructorBinder;
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.suppliedType = suppliedClass.getType();
        log.trace("Exiting NewSupplier constructor");
    }

    /**
     * Instantiates a new object using the constructor binder.
     *
     * @return the newly created instance, or {@link Optional#empty()} if a reflection
     *         error occurs during construction
     */
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