package com.garganttua.core.injection.context.properties;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

/**
 * Mutable, lifecycle-aware {@link IPropertyProvider} backed by a {@link ConcurrentHashMap},
 * with on-read coercion of stored values to the requested scalar type.
 */
public class PropertyProvider extends AbstractLifecycle implements IPropertyProvider {
    private static final Logger log = Logger.getLogger(PropertyProvider.class);

    private Map<String, Object> properties = new ConcurrentHashMap<>();

    /**
     * Creates a property provider seeded with the given properties.
     *
     * @param properties the initial properties; must not be {@code null}
     */
    public PropertyProvider(Map<String, Object> properties) {
        log.trace("Entering PropertyProvider constructor with properties: {}", properties);
        Objects.requireNonNull(properties, "Property map cannot be null");
        this.properties.putAll(properties);
        log.debug("Properties initialized with {} entries", properties.size());
        log.trace("Exiting PropertyProvider constructor");
    }

    /**
     * Retrieves a property by key, coercing it to the requested type when not already an instance.
     * Supported coercions: {@code String}, {@code Integer}, {@code Long}, {@code Double}, {@code Boolean}.
     *
     * @param key        the property key
     * @param type       the requested value type
     * @param <T>        the requested value type parameter
     * @return the value, or {@link Optional#empty()} if absent or not coercible
     * @throws DiException if coercion of a present value fails
     */
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

    /**
     * Stores or replaces a property value.
     *
     * @param key   the property key; must not be {@code null} or blank
     * @param value the property value
     * @throws DiException if this provider is immutable or the key is null/blank
     */
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

    /** {@return {@code true}; this provider always permits mutation} */
    @Override
    public boolean isMutable() {
        log.trace("Checking if PropertyProvider is mutable");
        return true;
    }

    /** {@return an unmodifiable view of all property keys} */
    @Override
    public Set<String> keys() {
        log.trace("Retrieving all property keys");
        return Collections.unmodifiableSet(properties.keySet());
    }

    /** {@return the ambient {@link IReflection} facade} */
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

    /**
     * Creates an independent copy of this provider with the same properties.
     *
     * @return the copied provider
     * @throws CopyException if the copy cannot be created
     */
    @Override
    public IPropertyProvider copy() throws CopyException {
        log.trace("Creating a copy of PropertyProvider");
        Map<String, Object> copiedMap = new ConcurrentHashMap<>(this.properties);
        PropertyProvider copy = new PropertyProvider(copiedMap);
        log.debug("Copy created with {} properties", copiedMap.size());
        return copy;
    }
}
