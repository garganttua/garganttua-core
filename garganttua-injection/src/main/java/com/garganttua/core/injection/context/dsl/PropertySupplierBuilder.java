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

@Reflected
public class PropertySupplierBuilder<Property> implements IPropertySupplierBuilder<Property> {
    private static final Logger log = Logger.getLogger(PropertySupplierBuilder.class);

    private IClass<Property> type;
    private String key;
    private String provider;

    public PropertySupplierBuilder(IClass<Property> type) {
        log.trace("Entering PropertySupplierBuilder constructor with type={}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.debug("PropertySupplierBuilder created for type={}", this.type.getSimpleName());
        log.trace("Exiting PropertySupplierBuilder constructor");
    }

    @Override
    public Type getSuppliedType() {
        log.trace("getSuppliedType() called, returning type={}", this.type);
        return this.type.getType();
    }

    @Override
    public IClass<Property> getSuppliedClass() {
        return this.type;
    }

    @Override
    public IPropertySupplier<Property> build() throws DslException {
        log.trace("Entering build() for PropertySupplierBuilder with key={} and provider={}", this.key, this.provider);
        IPropertySupplier<Property> supplier = new PropertySupplier<>(Optional.ofNullable(this.provider), this.key, this.type);
        log.debug("Built PropertySupplier for key={} and type={}", this.key, this.type.getSimpleName());
        log.trace("Exiting build()");
        return supplier;
    }

    @Override
    public IPropertySupplierBuilder<Property> key(String key) {
        log.trace("key() called with key={}", key);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        log.debug("Set key to {}", this.key);
        log.trace("Exiting key()");
        return this;
    }

    @Override
    public IPropertySupplierBuilder<Property> provider(String provider) {
        log.trace("provider() called with provider={}", provider);
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.debug("Set provider to {}", this.provider);
        log.trace("Exiting provider()");
        return this;
    }

    @Override
    public boolean isContextual() {
        log.trace("isContextual() called, returning false");
        return false;
    }
}
