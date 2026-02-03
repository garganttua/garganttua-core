package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullSupplierBuilder<SuppliedType>
        implements ISupplierBuilder<SuppliedType, ISupplier<SuppliedType>> {

    private Class<SuppliedType> class1;

    public NullSupplierBuilder(Class<SuppliedType> class1) {
        log.atTrace().log("Entering NullSupplierBuilder constructor with suppliedType: {}", class1);
        this.class1 = class1;
        log.atTrace().log("Exiting NullSupplierBuilder constructor");
    }

    @Override
    public ISupplier<SuppliedType> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building NullSupplier for type: {}", this.class1.getSimpleName());
        ISupplier<SuppliedType> result = new NullSupplier<>(this.class1);
        log.atDebug().log("Build completed for NullSupplier of type {}", this.class1.getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return this.class1;
    }

    @Override
    public boolean isContextual() {
        return false;
    }

    public static <SuppliedType> NullSupplierBuilder<SuppliedType> of(Class<SuppliedType> class1){
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating NullSupplierBuilder for type: {}", class1.getSimpleName());
        NullSupplierBuilder<SuppliedType> result = new NullSupplierBuilder<>(class1);
        log.atTrace().log("Exiting static of method");
        return result;
    }

}
