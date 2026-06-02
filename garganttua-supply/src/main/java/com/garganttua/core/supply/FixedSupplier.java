package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Supplier that always returns the same pre-supplied object.
 *
 * @param <Supplied> the type of object this supplier provides
 * @see ISupplier
 */
public class FixedSupplier<Supplied> implements ISupplier<Supplied> {
    private static final Logger log = Logger.getLogger(FixedSupplier.class);

    private Supplied object;
    private IClass<Supplied> suppliedClass;

    /**
     * Creates a fixed supplier wrapping the given object.
     *
     * @param object        the constant object to supply; must not be {@code null}
     * @param suppliedClass the {@link IClass} of the supplied object
     */
    public FixedSupplier(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering FixedSupplier constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.trace("Exiting FixedSupplier constructor");
    }

    @Override
    public Optional<Supplied> supply() throws SupplyException {
        log.trace("Entering supply method");
        log.debug("Supplying fixed object of type {}", this.object.getClass().getSimpleName());
        Optional<Supplied> result = Optional.of(this.object);
        log.debug("Supply completed for fixed object of type {}", this.object.getClass().getSimpleName());
        log.trace("Exiting supply method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

}
