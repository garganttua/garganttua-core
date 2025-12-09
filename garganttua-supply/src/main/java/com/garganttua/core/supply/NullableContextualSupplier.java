package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableContextualSupplier<SuppliedType, ContextType>
        implements IContextualSupplier<SuppliedType, ContextType> {

    private final IContextualSupplier<SuppliedType, ContextType> delegate;
    private final boolean allowNull;

    public NullableContextualSupplier(IContextualSupplier<SuppliedType, ContextType> delegate,
            boolean allowNull) {
        log.atTrace().log("Entering NullableContextualSupplier constructor with allowNull: {}", allowNull);
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        log.atTrace().log("Exiting NullableContextualSupplier constructor");
    }

    @Override
    public Type getSuppliedType() {
        return delegate.getSuppliedClass();
    }

    @Override
    public Class<ContextType> getOwnerContextType() {
        return this.delegate.getOwnerContextType();
    }

    @Override
    public Optional<SuppliedType> supply(ContextType ownerContext, Object... otherContexts) throws SupplyException {
        log.atTrace().log("Entering supply method with ownerContext: {}, otherContexts count: {}", ownerContext.getClass().getSimpleName(), otherContexts.length);
        log.atDebug().log("Supplying nullable contextual object for type {}, allowNull: {}", this.delegate.getSuppliedClass().getSimpleName(), this.allowNull);

        Optional<SuppliedType> o = delegate.supply(ownerContext, otherContexts);

        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier supplied null but is not nullable");
            log.atError().log("Supply failed: {}", msg);
            throw new SupplyException(msg);
        }

        Optional<SuppliedType> result = o == null ? Optional.empty() : o;
        log.atInfo().log("Supply completed for nullable contextual object of type {}, result present: {}", this.delegate.getSuppliedClass().getSimpleName(), result.isPresent());
        log.atTrace().log("Exiting supply method");
        return result;
    }

    public boolean isNullable() {
        return this.allowNull;
    }

    public ISupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}