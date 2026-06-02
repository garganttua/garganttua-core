package com.garganttua.core.configuration.integration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.configuration.IConfigurationNode;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.lifecycle.LifecycleStatus;

/**
 * Adapts a parsed configuration tree as an immutable {@link IPropertyProvider}, exposing
 * leaf values under flattened dot-notation keys (with {@code [index]} suffixes for arrays).
 */
public class ConfigurationPropertyProvider implements IPropertyProvider {
    private static final Logger log = Logger.getLogger(ConfigurationPropertyProvider.class);

    private final Map<String, String> properties;
    private LifecycleStatus status = LifecycleStatus.NEW;

    /**
     * Flattens the given configuration tree into dot-notation properties.
     *
     * @param root the root configuration node to flatten
     */
    public ConfigurationPropertyProvider(IConfigurationNode root) {
        this.properties = new LinkedHashMap<>();
        flatten(root, "");
        log.debug("Flattened configuration tree into {} properties", this.properties.size());
    }

    private ConfigurationPropertyProvider(Map<String, String> properties) {
        this.properties = new LinkedHashMap<>(properties);
    }

    private void flatten(IConfigurationNode node, String prefix) {
        if (node.isObject()) {
            for (var entry : node.children().entrySet()) {
                var key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                flatten(entry.getValue(), key);
            }
        } else if (node.isArray()) {
            var elements = node.elements();
            for (int i = 0; i < elements.size(); i++) {
                flatten(elements.get(i), prefix + "[" + i + "]");
            }
        } else if (node.isValue()) {
            node.asText().ifPresent(v -> this.properties.put(prefix, v));
        }
    }

    /**
     * Resolves a property by key, converting the stored string to the requested type.
     * Supports {@code String} and the boxed/primitive forms of {@code Integer}, {@code Long},
     * {@code Double}, and {@code Boolean}.
     *
     * @param key  the property key
     * @param type the target type
     * @param <T>  the target type parameter
     * @return the converted value, or empty if the key is absent
     * @throws DiException if the requested type is not supported
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getProperty(String key, IClass<T> type) throws DiException {
        var value = this.properties.get(key);
        if (value == null) {
            return Optional.empty();
        }
        var rawType = type.getType();
        if (rawType == String.class) {
            return Optional.of((T) value);
        }
        if (rawType == Integer.class || rawType == int.class) {
            return Optional.of((T) Integer.valueOf(value));
        }
        if (rawType == Long.class || rawType == long.class) {
            return Optional.of((T) Long.valueOf(value));
        }
        if (rawType == Double.class || rawType == double.class) {
            return Optional.of((T) Double.valueOf(value));
        }
        if (rawType == Boolean.class || rawType == boolean.class) {
            return Optional.of((T) Boolean.valueOf(value));
        }
        throw new DiException("Unsupported property type: " + type.getName());
    }

    /**
     * Unsupported: this provider is immutable.
     *
     * @throws DiException always, since the provider is read-only
     */
    @Override
    public void setProperty(String key, Object value) throws DiException {
        throw new DiException("ConfigurationPropertyProvider is immutable");
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Set<String> keys() {
        return Set.copyOf(this.properties.keySet());
    }

    @Override
    public IPropertyProvider copy() {
        return new ConfigurationPropertyProvider(this.properties);
    }

    @Override
    public ILifecycle onStart() throws LifecycleException {
        this.status = LifecycleStatus.STARTED;
        return this;
    }

    @Override
    public ILifecycle onStop() throws LifecycleException {
        this.status = LifecycleStatus.STOPPED;
        return this;
    }

    @Override
    public ILifecycle onFlush() throws LifecycleException {
        this.status = LifecycleStatus.FLUSHED;
        return this;
    }

    @Override
    public ILifecycle onInit() throws LifecycleException {
        this.status = LifecycleStatus.INITIALIZED;
        return this;
    }

    @Override
    public ILifecycle onReload() throws LifecycleException {
        return this;
    }

    @Override
    public LifecycleStatus status() {
        return this.status;
    }
}
