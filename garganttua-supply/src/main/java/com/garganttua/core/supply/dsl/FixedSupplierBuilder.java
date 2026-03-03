package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {

    private Supplied object;
    private IClass<Supplied> suppliedClass;

    public FixedSupplierBuilder(Supplied object, IClass<Supplied> suppliedClass) {
        log.atTrace().log("Entering FixedSupplierBuilder constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.atTrace().log("Exiting FixedSupplierBuilder constructor");
    }

    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building FixedSupplier for object type: {}", this.object.getClass().getSimpleName());
        ISupplier<Supplied> result = new FixedSupplier<>(this.object, this.suppliedClass);
        log.atDebug().log("Build completed for FixedSupplier of type {}", this.object.getClass().getSimpleName());
        log.atTrace().log("Exiting build method");
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
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating FixedSupplierBuilder for object type: {}", object.getClass().getSimpleName());
        ISupplierBuilder<Supplied, ISupplier<Supplied>> result = new FixedSupplierBuilder<>(object, suppliedClass);
        log.atTrace().log("Exiting static of method");
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> of(Supplied object) {
        return of(object, (IClass<Supplied>) IClass.getClass(object.getClass()));
    }

    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> ofNullable(Supplied object, IClass<Supplied> suppliedClass) {
        log.atTrace().log("Entering static ofNullable method");
        log.atDebug().log("Creating nullable builder for type: {}, object is null: {}", suppliedClass.getSimpleName(), object == null);

        if( object != null ) {
            log.atTrace().log("Exiting static ofNullable method with FixedSupplierBuilder");
            return new FixedSupplierBuilder<>(object, suppliedClass);
        }

        log.atTrace().log("Exiting static ofNullable method with NullSupplierBuilder");
        return new NullSupplierBuilder<>(suppliedClass);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
