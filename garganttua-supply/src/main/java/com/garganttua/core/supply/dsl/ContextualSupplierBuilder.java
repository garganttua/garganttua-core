package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ContextualSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualSupply;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualSupplierBuilder<Supplied, Context> implements ISupplierBuilder<Supplied, IContextualSupplier<Supplied, Context>> {

    private IContextualSupply<Supplied, Context> supply;
    private IClass<Supplied> suppliedClass;
    private IClass<Context> contextClass;

    public ContextualSupplierBuilder(IContextualSupply<Supplied, Context> supply,
            IClass<Supplied> suppliedClass, IClass<Context> contextClass) {
        log.atTrace().log("Entering ContextualSupplierBuilder constructor with suppliedClass: {}, contextClass: {}", suppliedClass, contextClass);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.contextClass = Objects.requireNonNull(contextClass, "Context class cannot be null");
        log.atTrace().log("Exiting ContextualSupplierBuilder constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedClass.getType();
    }

    @Override
    public IClass<Supplied> getSuppliedClass() {
        return this.suppliedClass;
    }

    @Override
    public IContextualSupplier<Supplied, Context> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building ContextualSupplier for suppliedClass: {}, contextClass: {}", this.suppliedClass.getSimpleName(), this.contextClass.getSimpleName());
        IContextualSupplier<Supplied, Context> result = new ContextualSupplier<>(this.supply, this.suppliedClass, this.contextClass);
        log.atDebug().log("Build completed for ContextualSupplier of type {}", this.suppliedClass.getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public boolean isContextual() {
        return true;
    }

}
