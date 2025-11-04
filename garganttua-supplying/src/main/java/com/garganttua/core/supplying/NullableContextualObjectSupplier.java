package com.garganttua.core.supplying;

import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableContextualObjectSupplier<SuppliedType, ContextType>
        implements IContextualObjectSupplier<SuppliedType, ContextType> {

    private final IContextualObjectSupplier<SuppliedType, ContextType> delegate;
    private final boolean allowNull;
    private final int index;
    private final String methodName;

    public NullableContextualObjectSupplier(IContextualObjectSupplier<SuppliedType, ContextType> delegate,
            boolean allowNull, int index, String methodName) {
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        this.index = index;
        this.methodName = methodName;
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
        Optional<SuppliedType> o = delegate.supply(ownerContext, otherContexts);
        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for parameter %d of method %s returned null but parameter is not nullable", index,
                    methodName);
            log.atError().log("[MethodBinderBuilder] " + msg);
            throw new SupplyException(msg);
        }
        return o == null ? Optional.empty() : o;
    }
}