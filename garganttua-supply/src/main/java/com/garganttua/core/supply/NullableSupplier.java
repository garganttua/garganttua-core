package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableSupplier<SuppliedType> implements ISupplier<SuppliedType> {
    private final ISupplier<SuppliedType> delegate;
    private final boolean allowNull;

    public NullableSupplier(ISupplier<SuppliedType> delegate, boolean allowNull) {
        log.atTrace().log("Entering NullableSupplier constructor with allowNull: {}", allowNull);
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        log.atTrace().log("Exiting NullableSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying nullable object for type {}, allowNull: {}", this.delegate.getSuppliedClass().getSimpleName(), this.allowNull);

        Optional<SuppliedType> o = delegate.supply();

        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for type "+this.delegate.getSuppliedClass().getSimpleName()+" supplied null value but is not nullable");
            log.atError().log("Supply failed: {}", msg);
            throw new SupplyException(msg);
        }

        Optional<SuppliedType> result = o == null ? Optional.empty() : o;
        log.atDebug().log("Supply completed for nullable object of type {}, result present: {}", this.delegate.getSuppliedClass().getSimpleName(), result.isPresent());
        log.atTrace().log("Exiting supply method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return delegate.getSuppliedClass();
    }

    public boolean isNullable() {
        return this.allowNull;
    }

    public ISupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}