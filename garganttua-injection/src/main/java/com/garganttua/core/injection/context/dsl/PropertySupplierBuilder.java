package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.properties.PropertySupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertySupplierBuilder<Property> implements IPropertySupplierBuilder<Property> {

    private Class<Property> type;
    private String key;
    private String provider;

    public PropertySupplierBuilder(Class<Property> type) {
        log.atTrace().log("Entering PropertySupplierBuilder constructor with type={}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.atDebug().log("PropertySupplierBuilder created for type={}", this.type.getSimpleName());
        log.atTrace().log("Exiting PropertySupplierBuilder constructor");
    }

    @Override
    public Type getSuppliedType() {
        log.atTrace().log("getSuppliedType() called, returning type={}", this.type);
        return this.type;
    }

    @Override
    public IPropertySupplier<Property> build() throws DslException {
        log.atTrace().log("Entering build() for PropertySupplierBuilder with key={} and provider={}", this.key, this.provider);
        IPropertySupplier<Property> supplier = new PropertySupplier<>(Optional.ofNullable(this.provider), this.key, this.type);
        log.atDebug().log("Built PropertySupplier for key={} and type={}", this.key, this.type.getSimpleName());
        log.atTrace().log("Exiting build()");
        return supplier;
    }

    @Override
    public IPropertySupplierBuilder<Property> key(String key) {
        log.atTrace().log("key() called with key={}", key);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        log.atDebug().log("Set key to {}", this.key);
        log.atTrace().log("Exiting key()");
        return this;
    }

    @Override
    public IPropertySupplierBuilder<Property> provider(String provider) {
        log.atTrace().log("provider() called with provider={}", provider);
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.atDebug().log("Set provider to {}", this.provider);
        log.atTrace().log("Exiting provider()");
        return this;
    }

    @Override
    public boolean isContextual() {
        log.atTrace().log("isContextual() called, returning false");
        return false;
    }
}