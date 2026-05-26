package com.garganttua.core.injection.context.properties;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

public class PropertyProvider extends AbstractLifecycle implements IPropertyProvider {
    private static final IDiagnostic log = Diagnostics.of(PropertyProvider.class);

    private Map<String, Object> properties = new ConcurrentHashMap<>();

    public PropertyProvider(Map<String, Object> properties) {
        log.trace("Entering PropertyProvider constructor with properties: {}", properties);
        Objects.requireNonNull(properties, "Property map cannot be null");
        this.properties.putAll(properties);
        log.debug("Properties initialized with {} entries", properties.size());
        log.trace("Exiting PropertyProvider constructor");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, IClass<T> type) throws DiException {
        log.trace("Entering getProperty with key: '{}' and type: {}", key, type);

        Object value = properties.get(key);
        if (value == null) {
            log.debug("Property '{}' not found", key);
            return Optional.empty();
        }

        if (!type.isInstance(value)) {
            log.debug("Property '{}' is not instance of {}, attempting conversion", key, type.getSimpleName());
            try {
                if (type.getName().equals(String.class.getName())) {
                    return Optional.of(type.cast(value.toString()));
                } else if (type.getName().equals(Integer.class.getName())) {
                    return Optional.of(type.cast(Integer.parseInt(value.toString())));
                } else if (type.getName().equals(Long.class.getName())) {
                    return Optional.of(type.cast(Long.parseLong(value.toString())));
                } else if (type.getName().equals(Double.class.getName())) {
                    return Optional.of(type.cast(Double.parseDouble(value.toString())));
                } else if (type.getName().equals(Boolean.class.getName())) {
                    return Optional.of(type.cast(Boolean.parseBoolean(value.toString())));
                }
            } catch (Exception e) {
                log.error("Failed to convert property '{}' value '{}' to type {}: {}", key, value, type.getSimpleName(), e.getMessage());
                throw new DiException(e.getMessage(), e);
            }
            log.warn("Property '{}' could not be converted to type {}, returning empty", key, type.getSimpleName());
            return Optional.empty();
        }

        log.trace("Property '{}' retrieved successfully: {}", key, value);
        return Optional.of((T) value);
    }

    @Override
    public void setProperty(String key, Object value) throws DiException {
        log.trace("Entering setProperty with key: '{}' and value: {}", key, value);

        if (!isMutable()) {
            log.error("Attempted to set property '{}' but PropertyProvider is not mutable", key);
            throw new DiException("PropertyProvider is not mutable");
        }
        if (key == null || key.isBlank()) {
            log.error("Attempted to set property with null or blank key");
            throw new DiException("Property key cannot be null or blank");
        }

        properties.put(key, value);
        log.debug("Property '{}' set with value '{}'", key, value);
        log.trace("Exiting setProperty for key: '{}'", key);
    }

    @Override
    public boolean isMutable() {
        log.trace("Checking if PropertyProvider is mutable");
        return true;
    }

    @Override
    public Set<String> keys() {
        log.trace("Retrieving all property keys");
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    public IReflection reflection() {
        return IClass.getReflection();
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        log.trace("Initializing PropertyProvider");
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        log.trace("Starting PropertyProvider");
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        log.debug("Flushing PropertyProvider: clearing all properties");
        this.properties.clear();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        log.trace("Stopping PropertyProvider");
        return this;
    }

    @Override
    public IPropertyProvider copy() throws CopyException {
        log.trace("Creating a copy of PropertyProvider");
        Map<String, Object> copiedMap = new ConcurrentHashMap<>(this.properties);
        PropertyProvider copy = new PropertyProvider(copiedMap);
        log.debug("Copy created with {} properties", copiedMap.size());
        return copy;
    }
}
