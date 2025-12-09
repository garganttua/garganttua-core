package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ContextualSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualObjectSupply;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualSupplierBuilder<Supplied, Context> implements ISupplierBuilder<Supplied, IContextualSupplier<Supplied, Context>> {

    private IContextualObjectSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public ContextualSupplierBuilder(IContextualObjectSupply<Supplied, Context> supply, Class<Supplied> suppliedType, Class<Context> contextType) {
        log.atTrace().log("Entering ContextualSupplierBuilder constructor with suppliedType: {}, contextType: {}", suppliedType, contextType);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
        log.atTrace().log("Exiting ContextualSupplierBuilder constructor");
    }

    @Override
    public Type getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public IContextualSupplier<Supplied, Context> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building ContextualSupplier for suppliedType: {}, contextType: {}", this.suppliedType.getSimpleName(), this.contextType.getSimpleName());
        IContextualSupplier<Supplied, Context> result = new ContextualSupplier<>(this.supply, this.suppliedType, this.contextType);
        log.atInfo().log("Build completed for ContextualSupplier of type {}", this.suppliedType.getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public boolean isContextual() {
        return true;
    }

}
