package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

@Reflected
public class FixedSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {
    private static final Logger log = Logger.getLogger(FixedSupplierBuilder.class);

    private Supplied object;
    private IClass<Supplied> suppliedClass;

    public FixedSupplierBuilder(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering FixedSupplierBuilder constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.trace("Exiting FixedSupplierBuilder constructor");
    }

    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.trace("Entering build method");
        log.debug("Building FixedSupplier for object type: {}", this.object.getClass().getSimpleName());
        ISupplier<Supplied> result = new FixedSupplier<>(this.object, this.suppliedClass);
        log.debug("Build completed for FixedSupplier of type {}", this.object.getClass().getSimpleName());
        log.trace("Exiting build method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> of(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering static of method");
        log.debug("Creating FixedSupplierBuilder for object type: {}", object.getClass().getSimpleName());
        ISupplierBuilder<Supplied, ISupplier<Supplied>> result = new FixedSupplierBuilder<>(object, suppliedClass);
        log.trace("Exiting static of method");
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> of(Supplied object) {
        return of(object, (IClass<Supplied>) IClass.getClass(object.getClass()));
    }

    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> ofNullable(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering static ofNullable method");
        log.debug("Creating nullable builder for type: {}, object is null: {}", suppliedClass.getSimpleName(), object == null);

        if( object != null ) {
            log.trace("Exiting static ofNullable method with FixedSupplierBuilder");
            return new FixedSupplierBuilder<>(object, suppliedClass);
        }

        log.trace("Exiting static ofNullable method with NullSupplierBuilder");
        return new NullSupplierBuilder<>(suppliedClass);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
