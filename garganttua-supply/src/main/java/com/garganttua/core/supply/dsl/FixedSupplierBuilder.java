package com.garganttua.core.supply.dsl;

import java.lang.reflect.Type;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.supply.FixedSupplier;
import com.garganttua.core.supply.ISupplier;

/**
 * Builder for creating {@link FixedSupplier} instances that always supply a
 * single, pre-set value.
 *
 * @param <Supplied> the type of object supplied by the built supplier
 * @since 2.0.0-ALPHA01
 * @see FixedSupplier
 */
@Reflected
public class FixedSupplierBuilder<Supplied>
        implements ISupplierBuilder<Supplied, ISupplier<Supplied>> {
    private static final Logger log = Logger.getLogger(FixedSupplierBuilder.class);

    private Supplied object;
    private IClass<Supplied> suppliedClass;

    /**
     * Creates a FixedSupplierBuilder.
     *
     * @param object the fixed value to supply, must not be {@code null}
     * @param suppliedClass the {@link IClass} of the supplied object
     */
    public FixedSupplierBuilder(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering FixedSupplierBuilder constructor with object type: {}", object.getClass().getSimpleName());
        this.object = Objects.requireNonNull(object, "Fixed object cannot be null");
        this.suppliedClass = Objects.requireNonNull(suppliedClass, "Supplied class cannot be null");
        log.trace("Exiting FixedSupplierBuilder constructor");
    }

    /**
     * Builds the configured {@link FixedSupplier}.
     *
     * @return a new fixed-value supplier
     * @throws DslException if the supplier cannot be constructed
     */
    @Override
    public ISupplier<Supplied> build() throws DslException {
        log.trace("Entering build method");
        log.debug("Building FixedSupplier for object type: {}", this.object.getClass().getSimpleName());
        ISupplier<Supplied> result = new FixedSupplier<>(this.object, this.suppliedClass);
        log.debug("Build completed for FixedSupplier of type {}", this.object.getClass().getSimpleName());
        log.trace("Exiting build method");
        return result;
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
     * Static factory creating a builder for a known supplied type.
     *
     * @param <Supplied> the type of object supplied
     * @param object the fixed value to supply
     * @param suppliedClass the {@link IClass} of the supplied object
     * @return a new builder instance
     */
    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> of(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering static of method");
        log.debug("Creating FixedSupplierBuilder for object type: {}", object.getClass().getSimpleName());
        ISupplierBuilder<Supplied, ISupplier<Supplied>> result = new FixedSupplierBuilder<>(object, suppliedClass);
        log.trace("Exiting static of method");
        return result;
    }

    /**
     * Static factory inferring the supplied type from the value's runtime class.
     *
     * @param <Supplied> the type of object supplied
     * @param object the fixed value to supply, must not be {@code null}
     * @return a new builder instance
     */
    @SuppressWarnings("unchecked")
    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> of(Supplied object) {
        return of(object, (IClass<Supplied>) IClass.getClass(object.getClass()));
    }

    /**
     * Static factory that builds a fixed supplier when {@code object} is non-null,
     * or a {@link NullSupplierBuilder} when it is {@code null}.
     *
     * @param <Supplied> the type of object supplied
     * @param object the value to supply, may be {@code null}
     * @param suppliedClass the {@link IClass} of the supplied object
     * @return a fixed or null supplier builder depending on {@code object}
     */
    public static <Supplied> ISupplierBuilder<Supplied, ISupplier<Supplied>> ofNullable(Supplied object, IClass<Supplied> suppliedClass) {
        log.trace("Entering static ofNullable method");
        log.debug("Creating nullable builder for type: {}, object is null: {}", suppliedClass.getSimpleName(), object == null);

        if( object != null ) {
            log.trace("Exiting static ofNullable method with FixedSupplierBuilder");
            return new FixedSupplierBuilder<>(object, suppliedClass);
        }

        log.trace("Exiting static ofNullable method with NullSupplierBuilder");
        return new NullSupplierBuilder<>(suppliedClass);
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
