package com.garganttua.core.injection.context.properties;

import static com.garganttua.core.lifecycle.AbstractLifecycle.*;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyProvider extends AbstractLifecycle implements IPropertyProvider {

    private Map<String, Object> properties = new ConcurrentHashMap<>();

    public PropertyProvider(Map<String, Object> properties) {
        log.atTrace().log("Entering PropertyProvider constructor with properties: {}", properties);
        Objects.requireNonNull(properties, "Property map cannot be null");
        this.properties.putAll(properties);
        log.atDebug().log("Properties initialized with {} entries", properties.size());
        log.atTrace().log("Exiting PropertyProvider constructor");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        log.atTrace().log("Entering getProperty with key: '{}' and type: {}", key, type);

        Object value = properties.get(key);
        if (value == null) {
            log.atInfo().log("Property '{}' not found", key);
            return Optional.empty();
        }

        if (!type.isInstance(value)) {
            log.atDebug().log("Property '{}' is not instance of {}, attempting conversion", key, type.getSimpleName());
            try {
                if (type.equals(String.class)) {
                    return Optional.of(type.cast(value.toString()));
                } else if (type.equals(Integer.class)) {
                    return Optional.of(type.cast(Integer.parseInt(value.toString())));
                } else if (type.equals(Long.class)) {
                    return Optional.of(type.cast(Long.parseLong(value.toString())));
                } else if (type.equals(Double.class)) {
                    return Optional.of(type.cast(Double.parseDouble(value.toString())));
                } else if (type.equals(Boolean.class)) {
                    return Optional.of(type.cast(Boolean.parseBoolean(value.toString())));
                }
            } catch (Exception e) {
                log.atError().log("Failed to convert property '{}' value '{}' to type {}: {}", key, value, type.getSimpleName(), e.getMessage());
                throw new DiException(e.getMessage(), e);
            }
            log.atWarn().log("Property '{}' could not be converted to type {}, returning empty", key, type.getSimpleName());
            return Optional.empty();
        }

        log.atTrace().log("Property '{}' retrieved successfully: {}", key, value);
        return Optional.of((T) value);
    }

    @Override
    public void setProperty(String key, Object value) throws DiException {
        log.atTrace().log("Entering setProperty with key: '{}' and value: {}", key, value);

        if (!isMutable()) {
            log.atError().log("Attempted to set property '{}' but PropertyProvider is not mutable", key);
            throw new DiException("PropertyProvider is not mutable");
        }
        if (key == null || key.isBlank()) {
            log.atError().log("Attempted to set property with null or blank key");
            throw new DiException("Property key cannot be null or blank");
        }

        properties.put(key, value);
        log.atInfo().log("Property '{}' set with value '{}'", key, value);
        log.atTrace().log("Exiting setProperty for key: '{}'", key);
    }

    @Override
    public boolean isMutable() {
        log.atTrace().log("Checking if PropertyProvider is mutable");
        return true;
    }

    @Override
    public Set<String> keys() {
        log.atTrace().log("Retrieving all property keys");
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    protected ILifecycle doInit() throws LifecycleException {
        log.atTrace().log("Initializing PropertyProvider");
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        log.atTrace().log("Starting PropertyProvider");
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        log.atInfo().log("Flushing PropertyProvider: clearing all properties");
        this.properties.clear();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        log.atTrace().log("Stopping PropertyProvider");
        return this;
    }

    @Override
    public IPropertyProvider copy() throws CopyException {
        log.atTrace().log("Creating a copy of PropertyProvider");
        Map<String, Object> copiedMap = new ConcurrentHashMap<>(this.properties);
        PropertyProvider copy = new PropertyProvider(copiedMap);
        log.atInfo().log("Copy created with {} properties", copiedMap.size());
        return copy;
    }
}
