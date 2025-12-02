package com.garganttua.core.supply.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.NullObjectSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullObjectSupplierBuilder<SuppliedType>
        implements IObjectSupplierBuilder<SuppliedType, IObjectSupplier<SuppliedType>> {

    private Class<SuppliedType> class1;

    public NullObjectSupplierBuilder(Class<SuppliedType> class1) {
        log.atTrace().log("Entering NullObjectSupplierBuilder constructor with suppliedType: {}", class1);
        this.class1 = class1;
        log.atTrace().log("Exiting NullObjectSupplierBuilder constructor");
    }

    @Override
    public IObjectSupplier<SuppliedType> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building NullObjectSupplier for type: {}", this.class1.getSimpleName());
        IObjectSupplier<SuppliedType> result = new NullObjectSupplier<>(this.class1);
        log.atInfo().log("Build completed for NullObjectSupplier of type {}", this.class1.getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.class1;
    }

    @Override
    public boolean isContextual() {
        return false;
    }

    public static <SuppliedType> NullObjectSupplierBuilder<SuppliedType> of(Class<SuppliedType> class1){
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating NullObjectSupplierBuilder for type: {}", class1.getSimpleName());
        NullObjectSupplierBuilder<SuppliedType> result = new NullObjectSupplierBuilder<>(class1);
        log.atTrace().log("Exiting static of method");
        return result;
    }

}
