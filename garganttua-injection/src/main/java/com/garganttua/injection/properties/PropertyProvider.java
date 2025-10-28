package com.garganttua.injection.properties;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.garganttua.injection.AbstractLifecycle;
import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.ILifecycle;
import com.garganttua.injection.spec.IPropertyProvider;

public class PropertyProvider extends AbstractLifecycle implements IPropertyProvider {

    private final Map<String, Object> properties = new ConcurrentHashMap<>();

    @Override
    public String getName() {
        return "garganttua";
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(String key, Class<T> type) {
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
                e.printStackTrace();
            }
            return Optional.empty();
        }

        return Optional.of((T) value);
    }

    @Override
    public void setProperty(String key, Object value) {
        if (!isMutable()) {
            throw new UnsupportedOperationException("PropertyProvider is not mutable");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Property key cannot be null or blank");
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
        properties.clear();
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
        properties.clear();
        return this;
    }

}
