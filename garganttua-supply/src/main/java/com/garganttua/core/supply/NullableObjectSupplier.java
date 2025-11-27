package com.garganttua.core.supply;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType> {
    private final IObjectSupplier<SuppliedType> delegate;
    private final boolean allowNull;

    public NullableObjectSupplier(IObjectSupplier<SuppliedType> delegate, boolean allowNull) {
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        Optional<SuppliedType> o = delegate.supply();
        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for type "+this.delegate.getSuppliedType().getSimpleName()+" supplied null value but is not nullable");
            log.atError().log("[MethodBinderBuilder] " + msg);
            throw new SupplyException(msg);
        }
        return o == null ? Optional.empty() : o;
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