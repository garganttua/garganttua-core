package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class NullSupplierBuilder<SuppliedType>
        implements ISupplierBuilder<SuppliedType, ISupplier<SuppliedType>> {
    private static final Logger log = Logger.getLogger(NullSupplierBuilder.class);

    private IClass<SuppliedType> suppliedClass;

    public NullSupplierBuilder(IClass<SuppliedType> suppliedClass) {
        log.trace("Entering NullSupplierBuilder constructor with suppliedClass: {}", suppliedClass);
        this.suppliedClass = suppliedClass;
        log.trace("Exiting NullSupplierBuilder constructor");
    }

    @Override
    public ISupplier<SuppliedType> build() throws DslException {
        log.trace("Entering build method");
        log.debug("Building NullSupplier for type: {}", this.suppliedClass.getSimpleName());
        ISupplier<SuppliedType> result = new NullSupplier<>(this.suppliedClass);
        log.debug("Build completed for NullSupplier of type {}", this.suppliedClass.getSimpleName());
        log.trace("Exiting build method");
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
        log.trace("Entering static of method");
        log.debug("Creating NullSupplierBuilder for type: {}", suppliedClass.getSimpleName());
        NullSupplierBuilder<SuppliedType> result = new NullSupplierBuilder<>(suppliedClass);
        log.trace("Exiting static of method");
        return result;
    }

}
