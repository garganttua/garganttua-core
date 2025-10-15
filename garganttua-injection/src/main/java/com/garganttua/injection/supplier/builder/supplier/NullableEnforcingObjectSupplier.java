package com.garganttua.injection.supplier.builder.supplier;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.supplier.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullableEnforcingObjectSupplier<T> implements IObjectSupplier<T> {
    private final IObjectSupplier<T> delegate;
    private final boolean allowNull;
    private final int index;
    private final String methodName;

    public NullableEnforcingObjectSupplier(IObjectSupplier<T> delegate, boolean allowNull, int index, String methodName) {
        this.delegate = Objects.requireNonNull(delegate);
        this.allowNull = allowNull;
        this.index = index;
        this.methodName = methodName;
    }

    @Override
    public Optional<T> getObject() throws DiException {
        Optional<T> o = delegate.getObject();
        if (!allowNull && (o == null || !o.isPresent())) {
            String msg = String.format(
                    "Supplier for parameter %d of method %s returned null but parameter is not nullable", index,
                    methodName);
            log.atError().log("[MethodBinderBuilder] " + msg);
            throw new DiException(msg);
        }
        return o == null ? Optional.empty() : o;
    }

    @Override
    public Class<T> getObjectClass() {
        return delegate.getObjectClass();
    }
}