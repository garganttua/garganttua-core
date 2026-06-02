package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Supplier that resolves values from a supplied owner context.
 *
 * <p>
 * Delegates to an {@link IContextualSupply} to produce values, validating that
 * the provided owner context is an instance of the declared context type before
 * each supply.
 * </p>
 *
 * @param <Supplied> the type of object this supplier provides
 * @param <Context>  the type of owner context required to supply a value
 * @see IContextualSupplier
 * @see IContextualSupply
 */
public class ContextualSupplier<Supplied, Context> implements IContextualSupplier<Supplied, Context> {
    private static final Logger log = Logger.getLogger(ContextualSupplier.class);

    private IContextualSupply<Supplied, Context> supply;
    private IClass<Supplied> suppliedClass;
    private IClass<Context> contextClass;

    /**
     * Creates a contextual supplier.
     *
     * @param supply        the underlying contextual supply that produces values
     * @param suppliedClass the {@link IClass} of the supplied object
     * @param contextClass  the {@link IClass} of the required owner context
     */
    public ContextualSupplier(IContextualSupply<Supplied, Context> supply,
            IClass<Supplied> suppliedClass, IClass<Context> contextClass) {
        log.trace("Entering ContextualSupplier constructor with suppliedClass: {}, contextClass: {}", suppliedClass, contextClass);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.contextClass = Objects.requireNonNull(contextClass, "Context class cannot be null");
        log.trace("Exiting ContextualSupplier constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Context> getOwnerContextType() {
        return this.contextClass;
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

    /**
     * Supplies a value using the given owner context.
     *
     * @param ownerContext  the owner context, validated against the declared context type
     * @param otherContexts additional contexts passed through to the underlying supply
     * @return the supplied value wrapped in an {@link Optional}
     * @throws SupplyException if {@code ownerContext} is not an instance of the declared
     *                         context type, or if the underlying supply fails
     */
    @Override
    public Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException {
        log.trace("Entering supply method with ownerContext: {}, otherContexts count: {}", ownerContext.getClass().getSimpleName(), otherContexts.length);
        log.debug("Supplying object of type {} with context type {}", this.suppliedClass.getSimpleName(), this.contextClass.getSimpleName());

        if (!this.contextClass.isInstance(ownerContext)) {
            log.error("Context type mismatch: expected {}, but got {}", this.contextClass.getSimpleName(), ownerContext.getClass().getSimpleName());
            throw new SupplyException("Context type mismatch : waiting " + this.contextClass.getSimpleName() + " but "
                    + ownerContext.getClass().getSimpleName() + " provided");
        }

        Optional<Supplied> result = this.supply.supply(ownerContext, otherContexts);
        log.debug("Supply completed for type {}, result present: {}", this.suppliedClass.getSimpleName(), result.isPresent());
        log.trace("Exiting supply method");
        return result;
    }

}
