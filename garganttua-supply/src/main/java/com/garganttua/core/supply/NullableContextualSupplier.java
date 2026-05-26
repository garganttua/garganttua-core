package com.garganttua.core.supply;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.reflection.IClass;

public class NullableContextualSupplier<SuppliedType, ContextType>
        implements IContextualSupplier<SuppliedType, ContextType> {
    private static final IDiagnostic log = Diagnostics.of(NullableContextualSupplier.class);

    private final IContextualSupplier<SuppliedType, ContextType> delegate;
    private final boolean allowNull;

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

    public boolean isNullable() {
        return this.allowNull;
    }

    public ISupplier<SuppliedType> getDelegate() {
        return this.delegate;
    }
}