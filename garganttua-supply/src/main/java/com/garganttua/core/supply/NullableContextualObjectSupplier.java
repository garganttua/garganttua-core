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
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
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
                    "Supplier supplied null but is not nullable");
            log.atError().log("[MethodBinderBuilder] " + msg);
            throw new SupplyException(msg);
        }
        return o == null ? Optional.empty() : o;
    }
}