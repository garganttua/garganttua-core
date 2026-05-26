package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ContextualSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class ContextualSupplierBuilder<Supplied, Context> implements ISupplierBuilder<Supplied, IContextualSupplier<Supplied, Context>> {
    private static final IDiagnostic log = Diagnostics.of(ContextualSupplierBuilder.class);

    private IContextualSupply<Supplied, Context> supply;
    private IClass<Supplied> suppliedClass;
    private IClass<Context> contextClass;

    public ContextualSupplierBuilder(IContextualSupply<Supplied, Context> supply,
            IClass<Supplied> suppliedClass, IClass<Context> contextClass) {
        log.trace("Entering ContextualSupplierBuilder constructor with suppliedClass: {}, contextClass: {}", suppliedClass, contextClass);
        this.supply = Objects.requireNonNull(supply, "Contextual supply cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        this.contextClass = Objects.requireNonNull(contextClass, "Context class cannot be null");
        log.trace("Exiting ContextualSupplierBuilder constructor");
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
        log.trace("Entering build method");
        log.debug("Building ContextualSupplier for suppliedClass: {}, contextClass: {}", this.suppliedClass.getSimpleName(), this.contextClass.getSimpleName());
        IContextualSupplier<Supplied, Context> result = new ContextualSupplier<>(this.supply, this.suppliedClass, this.contextClass);
        log.debug("Build completed for ContextualSupplier of type {}", this.suppliedClass.getSimpleName());
        log.trace("Exiting build method");
        return result;
    }

    @Override
    public boolean isContextual() {
        return true;
    }

}
