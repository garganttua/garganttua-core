package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.NullSupplier;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for creating {@link NullSupplier} instances that always supply an
 * empty value for a given type.
 *
 * @param <SuppliedType> the type the built supplier is declared to supply
 * @since 2.0.0-ALPHA01
 * @see NullSupplier
 */
@Reflected
public class NullSupplierBuilder<SuppliedType>
        implements ISupplierBuilder<SuppliedType, ISupplier<SuppliedType>> {
    private static final Logger log = Logger.getLogger(NullSupplierBuilder.class);

    private IClass<SuppliedType> suppliedClass;

    /**
     * Creates a NullSupplierBuilder.
     *
     * @param suppliedClass the {@link IClass} of the declared supplied type
     */
    public NullSupplierBuilder(IClass<SuppliedType> suppliedClass) {
        log.trace("Entering NullSupplierBuilder constructor with suppliedClass: {}", suppliedClass);
        this.suppliedClass = suppliedClass;
        log.trace("Exiting NullSupplierBuilder constructor");
    }

    /**
     * Builds the configured {@link NullSupplier}.
     *
     * @return a new null-value supplier
     * @throws DslException if the supplier cannot be constructed
     */
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

    /**
     * Static factory method for creating a NullSupplierBuilder.
     *
     * @param <SuppliedType> the declared supplied type
     * @param suppliedClass the {@link IClass} of the declared supplied type
     * @return a new builder instance
     */
    public static <SuppliedType> NullSupplierBuilder<SuppliedType> of(IClass<SuppliedType> suppliedClass){
        log.trace("Entering static of method");
        log.debug("Creating NullSupplierBuilder for type: {}", suppliedClass.getSimpleName());
        NullSupplierBuilder<SuppliedType> result = new NullSupplierBuilder<>(suppliedClass);
        log.trace("Exiting static of method");
        return result;
    }

}
