package com.garganttua.di.impl.supplier;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.injection.context.dsl.IPropertyProviderBuilder;
import com.garganttua.core.lifecycle.ILifecycle;

public class DummyPropertyProviderBuilder implements IPropertyProviderBuilder {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();
    private IDiContextBuilder parentBuilder;

    public DummyPropertyProviderBuilder() {
    }

    @Override
    public IPropertyProviderBuilder autoDetect(boolean b) throws DslException {
        // Pas d'auto-detection dans le dummy
        return this;
    }

    @Override
    public IDiContextBuilder up() {
        return this.parentBuilder;
    }

    @Override
    public IPropertyProviderBuilder setUp(IDiContextBuilder link) {
        this.parentBuilder = link;
        return this;
    }

    @Override
    public IPropertyProvider build() throws DslException {
        return new IPropertyProvider() {

            @Override
            public <T> java.util.Optional<T> getProperty(String key, Class<T> type) {
                Object value = properties.get(key);
                if (value != null && type.isInstance(value)) {
                    return java.util.Optional.of(type.cast(value));
                }
                return java.util.Optional.empty();
            }

            @Override
            public void setProperty(String key, Object value) {
                properties.put(key, value);
            }

            @Override
            public boolean isMutable() {
                return true;
            }

            @Override
            public ILifecycle onStart() throws DiException {
                return this;
            }

            @Override
            public ILifecycle onStop() throws DiException {
                return this;
            }

            @Override
            public ILifecycle onFlush() throws DiException {
                return this;
            }

            @Override
            public ILifecycle onInit() throws DiException {
                return this;
            }

            @Override
            public ILifecycle onReload() throws DiException {
                return this;
            }

            @Override
            public Set<String> keys() {
                return null;
            }
        };
    }

    @Override
    public <PropertyType> IPropertyProviderBuilder withProperty(Class<PropertyType> propertyType, String key,
            PropertyType property) throws DslException {
        properties.put(key, property);
        return this;
    }

}
