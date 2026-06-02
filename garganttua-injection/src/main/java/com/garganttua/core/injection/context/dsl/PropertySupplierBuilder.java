package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.properties.PropertySupplier;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for an {@link IPropertySupplier}, capturing the property key, value type, and
 * optional provider scope.
 *
 * @param <Property> the type of the property value supplied
 */
@Reflected
public class PropertySupplierBuilder<Property> implements IPropertySupplierBuilder<Property> {
    private static final Logger log = Logger.getLogger(PropertySupplierBuilder.class);

    private IClass<Property> type;
    private String key;
    private String provider;

    /**
     * Creates a property supplier builder for the given value type.
     *
     * @param type the property value type; must not be {@code null}
     */
    public PropertySupplierBuilder(IClass<Property> type) {
        log.trace("Entering PropertySupplierBuilder constructor with type={}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.debug("PropertySupplierBuilder created for type={}", this.type.getSimpleName());
        log.trace("Exiting PropertySupplierBuilder constructor");
    }

    /** {@return the reflective {@link Type} of the property value} */
    @Override
    public Type getSuppliedType() {
        log.trace("getSuppliedType() called, returning type={}", this.type);
        return this.type.getType();
    }

    /** {@return the {@link IClass} of the property value} */
    @Override
    public IClass<Property> getSuppliedClass() {
        return this.type;
    }

    /**
     * Builds the property supplier from the configured key, type, and optional provider.
     *
     * @return the built property supplier
     * @throws DslException if the supplier cannot be built
     */
    @Override
    public IPropertySupplier<Property> build() throws DslException {
        log.trace("Entering build() for PropertySupplierBuilder with key={} and provider={}", this.key, this.provider);
        IPropertySupplier<Property> supplier = new PropertySupplier<>(Optional.ofNullable(this.provider), this.key, this.type);
        log.debug("Built PropertySupplier for key={} and type={}", this.key, this.type.getSimpleName());
        log.trace("Exiting build()");
        return supplier;
    }

    /**
     * Sets the key identifying the property to supply.
     *
     * @param key the property key; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IPropertySupplierBuilder<Property> key(String key) {
        log.trace("key() called with key={}", key);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        log.debug("Set key to {}", this.key);
        log.trace("Exiting key()");
        return this;
    }

    /**
     * Sets the provider scope from which the property is resolved.
     *
     * @param provider the provider scope; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IPropertySupplierBuilder<Property> provider(String provider) {
        log.trace("provider() called with provider={}", provider);
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.debug("Set provider to {}", this.provider);
        log.trace("Exiting provider()");
        return this;
    }

    /** {@return {@code false}, as property suppliers are never contextual} */
    @Override
    public boolean isContextual() {
        log.trace("isContextual() called, returning false");
        return false;
    }
}
