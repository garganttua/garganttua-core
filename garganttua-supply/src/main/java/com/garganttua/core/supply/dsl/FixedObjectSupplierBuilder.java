package com.garganttua.core.supply.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.FixedObjectSupplier;
import com.garganttua.core.supply.IObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FixedObjectSupplierBuilder<Supplied>
        implements IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> {

    private Supplied object;

    public FixedObjectSupplierBuilder(Supplied object) {
        log.atTrace().log("Entering FixedObjectSupplierBuilder constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        log.atTrace().log("Exiting FixedObjectSupplierBuilder constructor");
    }

    @Override
    public IObjectSupplier<Supplied> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building FixedObjectSupplier for object type: {}", this.object.getClass().getSimpleName());
        IObjectSupplier<Supplied> result = new FixedObjectSupplier<>(this.object);
        log.atInfo().log("Build completed for FixedObjectSupplier of type {}", this.object.getClass().getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<Supplied> getSuppliedType() {
        return (Class<Supplied>) this.object.getClass();
    }

    public static <Supplied> IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> of(Supplied object) {
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating FixedObjectSupplierBuilder for object type: {}", object.getClass().getSimpleName());
        IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> result = new FixedObjectSupplierBuilder<>(object);
        log.atTrace().log("Exiting static of method");
        return result;
    }

    public static <Supplied> IObjectSupplierBuilder<Supplied, IObjectSupplier<Supplied>> ofNullable(Supplied object, Class<Supplied> type) {
        log.atTrace().log("Entering static ofNullable method");
        log.atDebug().log("Creating nullable builder for type: {}, object is null: {}", type.getSimpleName(), object == null);

        if( object != null ) {
            log.atTrace().log("Exiting static ofNullable method with FixedObjectSupplierBuilder");
            return new FixedObjectSupplierBuilder<>(object);
        }

        log.atTrace().log("Exiting static ofNullable method with NullObjectSupplierBuilder");
        return new NullObjectSupplierBuilder<>(type);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
