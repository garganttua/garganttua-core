package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IMethodReturn;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;

/**
 * Supplier that creates a new instance via a contextual constructor binder.
 *
 * <p>
 * Each {@link #supply(Void, Object...)} call invokes the
 * {@link IContextualConstructorBinder}, passing through the owner and any
 * additional contexts to the constructor.
 * </p>
 *
 * @param <SuppliedType> the type of object this supplier instantiates
 * @see IContextualSupplier
 * @see IContextualConstructorBinder
 */
public class NewContextualSupplier<SuppliedType>
        implements IContextualSupplier<SuppliedType, Void> {
    private static final Logger log = Logger.getLogger(NewContextualSupplier.class);

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;
    private IClass<Void> ownerContextClass;
    private IContextualConstructorBinder<SuppliedType> constructorBinder;

    /**
     * Creates a contextual instantiating supplier.
     *
     * @param suppliedClass     the {@link IClass} of the object to instantiate
     * @param ownerContextClass the {@link IClass} of the owner context
     * @param constructorBinder the contextual constructor binder used to build instances
     */
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

    /**
     * Instantiates a new object using the contextual constructor binder.
     *
     * @param ownerContext the owner context; must not be {@code null}
     * @param contexts     additional contexts passed to the constructor binder
     * @return the newly created instance wrapped in an {@link Optional}
     * @throws SupplyException if the binder returns an empty result, the constructor
     *                         throws, or a reflection error occurs
     */
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
