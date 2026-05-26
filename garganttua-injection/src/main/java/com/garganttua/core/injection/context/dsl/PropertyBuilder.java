package com.garganttua.core.injection.context.dsl;

import java.util.Map;
import java.util.Objects;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.annotations.Reflected;

@Reflected
public class PropertyBuilder<PropertyType> implements IPropertyBuilder<PropertyType> {
    private static final IDiagnostic log = Diagnostics.of(PropertyBuilder.class);

    private final String key;
    private final PropertyType property;

    public PropertyBuilder(String key, PropertyType property) {
        log.trace("Entering PropertyBuilder constructor with key={} and property={}", key, property);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.property = Objects.requireNonNull(property, "Property cannot be null");
        log.debug("PropertyBuilder created with key={} and property={}", this.key, this.property);
        log.trace("Exiting PropertyBuilder constructor");
    }

    @Override
    public Map.Entry<String, PropertyType> build() throws DslException {
        log.trace("Entering build() for key={}", this.key);
        Entry entry = new Entry(this.key, this.property);
        log.debug("Built Property Entry: {}", entry);
        log.trace("Exiting build()");
        return entry;
    }

    public class Entry implements Map.Entry<String, PropertyType> {

        private final String key;
        private PropertyType value;

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
