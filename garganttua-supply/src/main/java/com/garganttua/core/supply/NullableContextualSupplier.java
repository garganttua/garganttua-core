package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Decorator enforcing nullability rules on a contextual supplier.
 *
 * <p>
 * Delegates to the wrapped {@link IContextualSupplier} and, when {@code allowNull}
 * is {@code false}, rejects empty results by throwing a {@link SupplyException}.
 * </p>
 *
 * @param <SuppliedType> the type of object supplied
 * @param <ContextType>  the type of owner context required
 * @see IContextualSupplier
 */
public class NullableContextualSupplier<SuppliedType, ContextType>
        implements IContextualSupplier<SuppliedType, ContextType> {
    private static final Logger log = Logger.getLogger(NullableContextualSupplier.class);

    private final IContextualSupplier<SuppliedType, ContextType> delegate;
    private final boolean allowNull;

    /**
     * Wraps a contextual supplier with a nullability guard.
     *
     * @param delegate  the underlying contextual supplier
     * @param allowNull whether an empty/null result is permitted
     */
    public NullableContextualSupplier(IContextualSupplier<SuppliedType, ContextType> delegate,
            boolean allowNull) {
        log.trace("Entering NullableContextualSupplier constructor with allowNull: {}", allowNull);
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        log.trace("Exiting NullableContextualSupplier constructor");
    }

    @Override
    public Type getSuppliedType() {
        return delegate.getSuppliedType();
    }

    @Override
    public IClass<ContextType> getOwnerContextType() {
        return this.delegate.getOwnerContextType();
    }

    @Override
    public IClass<SuppliedType> getSuppliedClass() {
        return this.delegate.getSuppliedClass();
    }

    /**
     * Supplies a value through the delegate, enforcing nullability.
     *
     * @param ownerContext  the owner context passed to the delegate
     * @param otherContexts additional contexts passed to the delegate
     * @return the supplied value, or {@link Optional#empty()} when nulls are allowed
     * @throws SupplyException if the delegate yields no value and {@code allowNull} is
     *                         {@code false}, or if the delegate itself fails
     */
    @Override
    public Optional<SuppliedType> supply(ContextType ownerContext, Object... otherContexts) throws SupplyException {
        log.trace("Entering supply method with ownerContext: {}, otherContexts count: {}", ownerContext.getClass().getSimpleName(), otherContexts.length);
        log.debug("Supplying nullable contextual object for type {}, allowNull: {}", this.delegate.getSuppliedClass().getSimpleName(), this.allowNull);

        Optional<SuppliedType> o = delegate.supply(ownerContext, otherContexts);

        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier supplied null but is not nullable");
            log.error("Supply failed: {}", msg);
            throw new SupplyException(msg);
        }

        Optional<SuppliedType> result = o == null ? Optional.empty() : o;
        log.debug("Supply completed for nullable contextual object of type {}, result present: {}", this.delegate.getSuppliedClass().getSimpleName(), result.isPresent());
        log.trace("Exiting supply method");
        return result;
    }

    /**
     * Indicates whether empty/null results are permitted.
     *
     * @return {@code true} if nulls are allowed
     */
    public boolean isNullable() {
        return this.allowNull;
    }

    /**
     * Returns the wrapped contextual supplier.
     *
     * @return the delegate supplier
     */
    public ISupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}