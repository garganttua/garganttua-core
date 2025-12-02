package com.garganttua.core.supply.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.IContextualConstructorBinder;
import com.garganttua.core.supply.ContextualObjectSupplier;
import com.garganttua.core.supply.FixedObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupply;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.NewContextualObjectSupplier;
import com.garganttua.core.supply.NewObjectSupplier;
import com.garganttua.core.supply.NullObjectSupplier;
import com.garganttua.core.supply.NullableContextualObjectSupplier;
import com.garganttua.core.supply.NullableObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied> {

    private Class<?> contextType;
    private IContextualObjectSupply<Supplied, ?> supply;
    private Supplied value;
    private IConstructorBinder<Supplied> constructorBinder;
    private boolean nullable = false;
    private Class<Supplied> suppliedType;

    public SupplierBuilder(Class<Supplied> suppliedType) {
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
    }

    @Override
    public Class<Supplied> getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public boolean isContextual() {
        return this.contextType != null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public IObjectSupplier<Supplied> build() throws DslException {
        IObjectSupplier<Supplied> supplier;

        if (this.value != null) {
            supplier = new FixedObjectSupplier<>(this.value);
            return wrapNullable(supplier, this.nullable);
        }

        if (this.contextType != null) {
            if (this.constructorBinder != null) {
                if (!(this.constructorBinder instanceof IContextualConstructorBinder<Supplied> contextualCtorBinder)) {
                    throw new DslException(
                            "Context expected but constructor binder is not contextual: "
                                    + this.constructorBinder.getClass().getSimpleName());
                }

                supplier = new NewContextualObjectSupplier<>(this.suppliedType, contextualCtorBinder);

            } else {
                supplier = new ContextualObjectSupplier(this.supply, this.suppliedType, this.contextType);
            }

            return wrapNullableContextual(
                    (IContextualObjectSupplier<Supplied, ?>) supplier,
                    this.nullable);
        }

        if (this.constructorBinder != null) {
            supplier = new NewObjectSupplier<>(this.suppliedType, this.constructorBinder);
            return wrapNullable(supplier, this.nullable);
        }

        supplier = new NullObjectSupplier<>(this.suppliedType);
        return wrapNullable(supplier, true);
    }

    @Override
    public ISupplierBuilder<Supplied> nullable(boolean nullable) {
        this.nullable = Objects.requireNonNull(nullable, "Nullable cannot be null");
        return this;
    }

    @Override
    public <ContextType> ISupplierBuilder<Supplied> withContext(
            Class<ContextType> contextType,
            IContextualObjectSupply<Supplied, ContextType> supply) throws DslException {
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
        this.supply = Objects.requireNonNull(supply, "Supply cannot be null");
        return this;

    }

    @Override
    public ISupplierBuilder<Supplied> withValue(Supplied value) throws DslException {
        this.value = value;
        return this;
    }

    @Override
    public ISupplierBuilder<Supplied> withConstructor(
            IConstructorBinder<Supplied> constructorBinder)
            throws DslException {
        this.constructorBinder = Objects.requireNonNull(constructorBinder, "Constructor binder cannot be null");
        return this;
    }

    private IObjectSupplier<Supplied> wrapNullable(IObjectSupplier<Supplied> supplier, boolean nullable) {
        return new NullableObjectSupplier<>(supplier, nullable);
    }

    private IObjectSupplier<Supplied> wrapNullableContextual(
            IContextualObjectSupplier<Supplied, ?> supplier, boolean nullable) {

        return new NullableContextualObjectSupplier<>(supplier, nullable);
    }

    public static <T> ISupplierBuilder<T> fixed(Class<T> type, T value) {
        return new SupplierBuilder<>(type).withValue(value).nullable(false);
    }

    public static <T> ISupplierBuilder<T> newObject(Class<T> type, IConstructorBinder<T> binder) {
        return new SupplierBuilder<>(type).withConstructor(binder);
    }

    public static <T> ISupplierBuilder<T> nullObject(Class<T> type) {
        return new SupplierBuilder<>(type).nullable(true);
    }

    public static <T, C> ISupplierBuilder<T> contextual(
            Class<T> type,
            Class<C> contextType,
            IContextualObjectSupply<T, C> supply) throws DslException {

        return new SupplierBuilder<>(type).withContext(contextType, supply);
    }

    public static <T, C> ISupplierBuilder<T> newContextual(
            Class<T> type,
            Class<C> contextType,
            IContextualConstructorBinder<T> binder) {

        SupplierBuilder<T> builder = new SupplierBuilder<>(type);
        builder.contextType = contextType;
        builder.constructorBinder = binder;
        return builder;
    }

}
