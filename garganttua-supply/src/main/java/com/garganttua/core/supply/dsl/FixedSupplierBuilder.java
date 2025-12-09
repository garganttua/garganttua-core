package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {

    private Supplied object;

    public FixedSupplierBuilder(Supplied object) {
        log.atTrace().log("Entering FixedSupplierBuilder constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        log.atTrace().log("Exiting FixedSupplierBuilder constructor");
    }

    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building FixedSupplier for object type: {}", this.object.getClass().getSimpleName());
        ISupplier<Supplied> result = new FixedSupplier<>(this.object);
        log.atInfo().log("Build completed for FixedSupplier of type {}", this.object.getClass().getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Type getSuppliedType() {
        return (Class<Supplied>) this.object.getClass();
    }

    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> of(Supplied object) {
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating FixedSupplierBuilder for object type: {}", object.getClass().getSimpleName());
        ISupplierBuilder<Supplied, ISupplier<Supplied>> result = new FixedSupplierBuilder<>(object);
        log.atTrace().log("Exiting static of method");
        return result;
    }

    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> ofNullable(Supplied object, Class<Supplied> type) {
        log.atTrace().log("Entering static ofNullable method");
        log.atDebug().log("Creating nullable builder for type: {}, object is null: {}", type.getSimpleName(), object == null);

        if( object != null ) {
            log.atTrace().log("Exiting static ofNullable method with FixedSupplierBuilder");
            return new FixedSupplierBuilder<>(object);
        }

        log.atTrace().log("Exiting static ofNullable method with NullSupplierBuilder");
        return new NullSupplierBuilder<>(type);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
