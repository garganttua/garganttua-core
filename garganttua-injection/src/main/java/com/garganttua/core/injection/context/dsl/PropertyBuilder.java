package com.garganttua.core.injection.context.dsl;

import java.util.Map;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertyBuilder<PropertyType> implements IPropertyBuilder<PropertyType> {

    private final String key;
    private final PropertyType property;

    public PropertyBuilder(String key, PropertyType property) {
        log.atTrace().log("Entering PropertyBuilder constructor with key={} and property={}", key, property);
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.property = Objects.requireNonNull(property, "Property cannot be null");
        log.atDebug().log("PropertyBuilder created with key={} and property={}", this.key, this.property);
        log.atTrace().log("Exiting PropertyBuilder constructor");
    }

    @Override
    public Map.Entry<String, PropertyType> build() throws DslException {
        log.atTrace().log("Entering build() for key={}", this.key);
        Entry entry = new Entry(this.key, this.property);
        log.atDebug().log("Built Property Entry: {}", entry);
        log.atTrace().log("Exiting build()");
        return entry;
    }

    public class Entry implements Map.Entry<String, PropertyType> {

        private final String key;
        private PropertyType value;

        public Entry(String key, PropertyType value) {
            log.atTrace().log("Entering Entry constructor with key={} and value={}", key, value);
            this.key = key;
            this.value = value;
            log.atDebug().log("Entry created with key={} and value={}", this.key, this.value);
            log.atTrace().log("Exiting Entry constructor");
        }

        @Override
        public String getKey() {
            log.atTrace().log("getKey() called, returning key={}", key);
            return key;
        }

        @Override
        public PropertyType getValue() {
            log.atTrace().log("getValue() called, returning value={}", value);
            return value;
        }

        @Override
        public PropertyType setValue(PropertyType value) {
            log.atTrace().log("setValue() called with value={}", value);
            Objects.requireNonNull(value, "Property value cannot be null");
            PropertyType old = this.value;
            this.value = value;
            log.atDebug().log("Value updated from {} to {}", old, this.value);
            log.atTrace().log("Exiting setValue() with old value={}", old);
            return old;
        }

        @Override
        public String toString() {
            String str = key + "=" + value;
            log.atTrace().log("toString() called, returning {}", str);
            return str;
        }
    }

}
