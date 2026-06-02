package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Supplier that always supplies an empty result for a given type.
 *
 * @param <SuppliedType> the declared type of object this supplier represents
 * @see ISupplier
 */
public class NullSupplier<SuppliedType> implements ISupplier<SuppliedType>{
    private static final Logger log = Logger.getLogger(NullSupplier.class);

    private Type suppliedType;
    private IClass<SuppliedType> suppliedClass;

    /**
     * Creates a null supplier for the given type.
     *
     * @param suppliedClass the {@link IClass} representing the supplied type
     */
    public NullSupplier(IClass<SuppliedType> suppliedClass) {
        this.suppliedType = suppliedClass.getType();
        this.suppliedClass = suppliedClass;
    }

    /**
     * Supplies an empty result.
     *
     * @return always {@link Optional#empty()}
     */
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
