package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IPropertySupplier;
import com.garganttua.core.injection.context.dsl.IPropertySupplierBuilder;

public class PropertySupplierBuilder<Property> implements IPropertySupplierBuilder<Property>{

    private Class<Property> type;
    private String key;
    private String provider;

    public PropertySupplierBuilder(Class<Property> type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    @Override
    public Class<Property> getObjectClass() {
        return this.type;
    }

    @Override
    public IPropertySupplier<Property> build() throws DslException {
        return new PropertySupplier<Property>(Optional.ofNullable(this.provider), this.key, this.type);
    }

    @Override
    public IPropertySupplierBuilder<Property> key(String key) {
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        return this;
    }

    @Override
    public IPropertySupplierBuilder<Property> provider(String provider) {
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        return this;
    }

}
