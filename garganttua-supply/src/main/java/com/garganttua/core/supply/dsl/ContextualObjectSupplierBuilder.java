package com.garganttua.core.supply.dsl;

import java.util.Objects;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.supply.ContextualObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupplier;
import com.garganttua.core.supply.IContextualObjectSupply;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContextualObjectSupplierBuilder<Supplied, Context> implements IObjectSupplierBuilder<Supplied, IContextualObjectSupplier<Supplied, Context>> {

    private IContextualObjectSupply<Supplied, Context> supply;
    private Class<Supplied> suppliedType;
    private Class<Context> contextType;

    public ContextualObjectSupplierBuilder(IContextualObjectSupply<Supplied, Context> supply, Class<Supplied> suppliedType, Class<Context> contextType) {
        log.atTrace().log("Entering ContextualObjectSupplierBuilder constructor with suppliedType: {}, contextType: {}", suppliedType, contextType);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedType = Objects.requireNonNull(suppliedType, "Supplied type cannot be null");
        this.contextType = Objects.requireNonNull(contextType, "Context type cannot be null");
        log.atTrace().log("Exiting ContextualObjectSupplierBuilder constructor");
    }

    @Override
    public Class<Supplied> getSuppliedType() {
        return this.suppliedType;
    }

    @Override
    public IContextualObjectSupplier<Supplied, Context> build() throws DslException {
        log.atTrace().log("Entering build method");
        log.atDebug().log("Building ContextualObjectSupplier for suppliedType: {}, contextType: {}", this.suppliedType.getSimpleName(), this.contextType.getSimpleName());
        IContextualObjectSupplier<Supplied, Context> result = new ContextualObjectSupplier<>(this.supply, this.suppliedType, this.contextType);
        log.atInfo().log("Build completed for ContextualObjectSupplier of type {}", this.suppliedType.getSimpleName());
        log.atTrace().log("Exiting build method");
        return result;
    }

    @Override
    public boolean isContextual() {
        return true;
    }

}
