package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;

public class NullableSupplier<SuppliedType> implements ISupplier<SuppliedType> {
    private static final IDiagnostic log = Diagnostics.of(NullableSupplier.class);

    private final ISupplier<SuppliedType> delegate;
    private final boolean allowNull;

    public NullableSupplier(ISupplier<SuppliedType> delegate, boolean allowNull) {
        log.trace("Entering NullableSupplier constructor with allowNull: {}", allowNull);
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        log.trace("Exiting NullableSupplier constructor");
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.trace("Entering supply method");
        log.debug("Supplying nullable object for type {}, allowNull: {}", this.delegate.getSuppliedClass().getSimpleName(), this.allowNull);

        Optional<SuppliedType> o = delegate.supply();

        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for type "+this.delegate.getSuppliedClass().getSimpleName()+" supplied null value but is not nullable");
            log.error("Supply failed: {}", msg);
            throw new SupplyException(msg);
        }

        Optional<SuppliedType> result = o == null ? Optional.empty() : o;
        log.debug("Supply completed for nullable object of type {}, result present: {}", this.delegate.getSuppliedClass().getSimpleName(), result.isPresent());
        log.trace("Exiting supply method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return delegate.getSuppliedType();
    }

    @Override
    public IClass<SuppliedType> getSuppliedClass() {
        return delegate.getSuppliedClass();
    }

    public boolean isNullable() {
        return this.allowNull;
    }

    public ISupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}