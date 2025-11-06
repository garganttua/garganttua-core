package com.garganttua.core.injection.context.dsl;

import java.util.Map;
import java.util.Objects;

import com.garganttua.core.dsl.DslException;

public class PropertyBuilder<PropertyType> implements IPropertyBuilder<PropertyType> {

    private final String key;
    private final PropertyType property;

    public PropertyBuilder(String key, PropertyType property) {
        this.key = Objects.requireNonNull(key, "Key cannot be null");
        this.property = Objects.requireNonNull(property, "Property cannot be null");
    }

    @Override
    public Map.Entry<String, PropertyType> build() throws DslException {
        return new Entry(this.key, this.property);
    }

    public class Entry implements Map.Entry<String, PropertyType> {

        private final String key;
        private PropertyType value;

        public Entry(String key, PropertyType value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public PropertyType getValue() {
            return value;
        }

        @Override
        public PropertyType setValue(PropertyType value) {
            Objects.requireNonNull(value, "Property value cannot be null");
            PropertyType old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

}
