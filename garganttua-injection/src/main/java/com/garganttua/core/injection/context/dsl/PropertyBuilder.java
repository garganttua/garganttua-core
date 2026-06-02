package com.garganttua.core.injection.context.dsl;

import java.util.Map;
import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for a single property, producing a {@link java.util.Map.Entry} pairing a key with its value.
 *
 * @param <PropertyType> the type of the property value
 */
@Reflected
public class PropertyBuilder<PropertyType> implements IPropertyBuilder<PropertyType> {
    private static final Logger log = Logger.getLogger(PropertyBuilder.class);

    private final String key;
    private final PropertyType property;

    /**
     * Creates a property builder for the given key and value.
     *
     * @param key      the property key; must not be {@code null}
     * @param property the property value; must not be {@code null}
     */
    public PropertyBuilder(String key, PropertyType property) {
        log.trace("Entering PropertyBuilder constructor with key={} and property={}", key, property);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.property = Objects.requireNonNull(property, "Property cannot be null");
        log.debug("PropertyBuilder created with key={} and property={}", this.key, this.property);
        log.trace("Exiting PropertyBuilder constructor");
    }

    /**
     * Builds the property as a mutable {@link java.util.Map.Entry}.
     *
     * @return the key/value entry
     * @throws DslException if the entry cannot be built
     */
    @Override
    public Map.Entry<String, PropertyType> build() throws DslException {
        log.trace("Entering build() for key={}", this.key);
        Entry entry = new Entry(this.key, this.property);
        log.debug("Built Property Entry: {}", entry);
        log.trace("Exiting build()");
        return entry;
    }

    /** Mutable {@link java.util.Map.Entry} implementation backing a built property. */
    public class Entry implements Map.Entry<String, PropertyType> {

        private final String key;
        private PropertyType value;

        /**
         * Creates a property entry.
         *
         * @param key   the property key
         * @param value the property value
         */
        public Entry(String key, PropertyType value) {
            log.trace("Entering Entry constructor with key={} and value={}", key, value);
            this.key = key;
            this.value = value;
            log.debug("Entry created with key={} and value={}", this.key, this.value);
            log.trace("Exiting Entry constructor");
        }

        @Override
        public String getKey() {
            log.trace("getKey() called, returning key={}", key);
            return key;
        }

        @Override
        public PropertyType getValue() {
            log.trace("getValue() called, returning value={}", value);
            return value;
        }

        @Override
        public PropertyType setValue(PropertyType value) {
            log.trace("setValue() called with value={}", value);
            Objects.requireNonNull(value, "Property value cannot be null");
            PropertyType old = this.value;
            this.value = value;
            log.debug("Value updated from {} to {}", old, this.value);
            log.trace("Exiting setValue() with old value={}", old);
            return old;
        }

        @Override
        public String toString() {
            String str = key + "=" + value;
            log.trace("toString() called, returning {}", str);
            return str;
        }
    }

}
