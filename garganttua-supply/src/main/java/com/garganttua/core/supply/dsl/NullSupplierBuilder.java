package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NullSupplierBuilder<SuppliedType>
        implements ISupplierBuilder<SuppliedType, ISupplier<SuppliedType>> {

    private IClass<SuppliedType> suppliedClass;

    public NullSupplierBuilder(IClass<SuppliedType> suppliedClass) {
        log.atTrace().log("Entering NullSupplierBuilder constructor with suppliedClass: {}", suppliedClass);
        this.suppliedClass = suppliedClass;
        log.atTrace().log("Exiting NullSupplierBuilder constructor");
    }

    @Override
    public ISupplier<SuppliedType> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building NullSupplier for type: {}", this.suppliedClass.getSimpleName());
        ISupplier<SuppliedType> result = new NullSupplier<>(this.suppliedClass);
        log.atDebug().log("Build completed for NullSupplier of type {}", this.suppliedClass.getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<SuppliedType> getSuppliedClass() {
        return this.suppliedClass;
    }

    @Override
    public boolean isContextual() {
        return false;
    }

    public static <SuppliedType> NullSupplierBuilder<SuppliedType> of(IClass<SuppliedType> suppliedClass){
        log.atTrace().log("Entering static of method");
        log.atDebug().log("Creating NullSupplierBuilder for type: {}", suppliedClass.getSimpleName());
        NullSupplierBuilder<SuppliedType> result = new NullSupplierBuilder<>(suppliedClass);
        log.atTrace().log("Exiting static of method");
        return result;
    }

}
