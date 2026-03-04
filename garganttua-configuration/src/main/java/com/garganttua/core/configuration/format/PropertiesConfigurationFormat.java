package com.garganttua.core.configuration.format;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.reflection.IClass;

public class PropertiesConfigurationFormat extends AbstractConfigurationFormat {

    private static final String PROPS_FACTORY_CLASS = "com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory";

    @Override
    public String name() {
        return "properties";
    }

    @Override
    public Set<String> extensions() {
        return Set.of("properties");
    }

    @Override
    public Set<String> mediaTypes() {
        return Set.of("text/x-java-properties");
    }

    @Override
    protected ObjectMapper createMapper() {
        try {
            IClass<?> factoryClass = IClass.forName(PROPS_FACTORY_CLASS);
            var factory = factoryClass.getDeclaredConstructor().newInstance();
            return new ObjectMapper((com.fasterxml.jackson.core.JsonFactory) factory);
        } catch (Exception e) {
            throw new IllegalStateException("Properties support not available", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return isClassAvailable(PROPS_FACTORY_CLASS);
    }
}
