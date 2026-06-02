package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ContextualSupplier;
import com.garganttua.core.supply.IContextualSupplier;
import com.garganttua.core.supply.IContextualSupply;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for creating {@link ContextualSupplier} instances.
 *
 * <p>
 * This builder wires an {@link IContextualSupply} together with the supplied and
 * context types to produce a context-aware supplier.
 * </p>
 *
 * @param <Supplied> the type of object supplied by the built supplier
 * @param <Context> the type of context consumed when supplying
 * @since 2.0.0-ALPHA01
 * @see ContextualSupplier
 */
@Reflected
public class ContextualSupplierBuilder<Supplied, Context> implements ISupplierBuilder<Supplied, IContextualSupplier<Supplied, Context>> {
    private static final Logger log = Logger.getLogger(ContextualSupplierBuilder.class);

    private IContextualSupply<Supplied, Context> supply;
    private IClass<Supplied> suppliedClass;
    private IClass<Context> contextClass;

    /**
     * Creates a ContextualSupplierBuilder.
     *
     * @param supply the contextual supply function invoked at supply time
     * @param suppliedClass the {@link IClass} of the supplied object
     * @param contextClass the {@link IClass} of the context
     */
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

    /**
     * Builds the configured {@link ContextualSupplier}.
     *
     * @return a new contextual supplier
     * @throws DslException if the supplier cannot be constructed
     */
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
