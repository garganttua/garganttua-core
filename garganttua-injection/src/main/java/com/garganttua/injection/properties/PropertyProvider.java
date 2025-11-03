package com.garganttua.injection.properties;

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

public class PropertyProvider extends AbstractLifecycle implements IPropertyProvider {

    private Map<String, Object> properties = new ConcurrentHashMap<>();

    public PropertyProvider(Map<String, Object> properties) {
        Objects.requireNonNull(properties, "Property map cannot be null");
        this.properties.putAll(properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        Object value = properties.get(key);
        if (value == null) {
            return Optional.empty();
        }
 
        if (!type.isInstance(value)) {
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
                throw new DiException(e.getMessage(), e);
            }
            return Optional.empty();
        }

        return Optional.of((T) value);
    }

    @Override
    public void setProperty(String key, Object value) throws DiException {
        if (!isMutable()) {
            throw new DiException("PropertyProvider is not mutable");
        }
        if (key == null || key.isBlank()) {
            throw new DiException("Property key cannot be null or blank");
        }
        properties.put(key, value);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(properties.keySet());
    }

    @Override
    protected ILifecycle doInit() throws DiException {
        return this;
    }

    @Override
    protected ILifecycle doStart() throws DiException {
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws DiException {
        properties.clear();
        return this;
    }

    @Override
    protected ILifecycle doStop() throws DiException {
        return this;
    }

}
