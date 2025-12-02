package com.garganttua.core.supply;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType> {
    private final IObjectSupplier<SuppliedType> delegate;
    private final boolean allowNull;

    public NullableObjectSupplier(IObjectSupplier<SuppliedType> delegate, boolean allowNull) {
        log.atTrace().log("Entering NullableObjectSupplier constructor with allowNull: {}", allowNull);
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        log.atTrace().log("Exiting NullableObjectSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying nullable object for type {}, allowNull: {}", this.delegate.getSuppliedType().getSimpleName(), this.allowNull);

        Optional<SuppliedType> o = delegate.supply();

        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for type "+this.delegate.getSuppliedType().getSimpleName()+" supplied null value but is not nullable");
            log.atError().log("Supply failed: {}", msg);
            throw new SupplyException(msg);
        }

        Optional<SuppliedType> result = o == null ? Optional.empty() : o;
        log.atInfo().log("Supply completed for nullable object of type {}, result present: {}", this.delegate.getSuppliedType().getSimpleName(), result.isPresent());
        log.atTrace().log("Exiting supply method");
        return result;
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return delegate.getSuppliedType();
    }

    public boolean isNullable() {
        return this.allowNull;
    }

    public IObjectSupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}