package com.garganttua.core.supply;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableContextualObjectSupplier<SuppliedType, ContextType>
        implements IContextualObjectSupplier<SuppliedType, ContextType> {

    private final IContextualObjectSupplier<SuppliedType, ContextType> delegate;
    private final boolean allowNull;

    public NullableContextualObjectSupplier(IContextualObjectSupplier<SuppliedType, ContextType> delegate,
            boolean allowNull) {
        log.atTrace().log("Entering NullableContextualObjectSupplier constructor with allowNull: {}", allowNull);
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        log.atTrace().log("Exiting NullableContextualObjectSupplier constructor");
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return delegate.getSuppliedType();
    }

    @Override
    public Class<ContextType> getOwnerContextType() {
        return this.delegate.getOwnerContextType();
    }

    @Override
    public Optional<SuppliedType> supply(ContextType ownerContext, Object... otherContexts) throws SupplyException {
        log.atTrace().log("Entering supply method with ownerContext: {}, otherContexts count: {}", ownerContext.getClass().getSimpleName(), otherContexts.length);
        log.atDebug().log("Supplying nullable contextual object for type {}, allowNull: {}", this.delegate.getSuppliedType().getSimpleName(), this.allowNull);

        Optional<SuppliedType> o = delegate.supply(ownerContext, otherContexts);

        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier supplied null but is not nullable");
            log.atError().log("Supply failed: {}", msg);
            throw new SupplyException(msg);
        }

        Optional<SuppliedType> result = o == null ? Optional.empty() : o;
        log.atInfo().log("Supply completed for nullable contextual object of type {}, result present: {}", this.delegate.getSuppliedType().getSimpleName(), result.isPresent());
        log.atTrace().log("Exiting supply method");
        return result;
    }

    public boolean isNullable() {
        return this.allowNull;
    }

    public IObjectSupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}