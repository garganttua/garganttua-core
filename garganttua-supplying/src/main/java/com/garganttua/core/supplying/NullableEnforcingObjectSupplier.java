package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableEnforcingObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType> {
    private final IObjectSupplier<SuppliedType> delegate;
    private final boolean allowNull;
    private final int index;
    private final String methodName;

    public NullableEnforcingObjectSupplier(IObjectSupplier<SuppliedType> delegate, boolean allowNull, int index, String methodName) {
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        this.index = index;
        this.methodName = methodName;
    }

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        Optional<SuppliedType> o = delegate.supply();
        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for parameter %d of method %s returned null but parameter is not nullable", index,
                    methodName);
            log.atError().log("[MethodBinderBuilder] " + msg);
            throw new SupplyException(msg);
        }
        return o == null ? Optional.empty() : o;
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return delegate.getSuppliedType();
    }
}