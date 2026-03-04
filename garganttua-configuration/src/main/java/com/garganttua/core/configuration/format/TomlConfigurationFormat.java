package com.garganttua.core.configuration.format;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.reflection.IClass;

public class TomlConfigurationFormat extends AbstractConfigurationFormat {

    private static final String TOML_FACTORY_CLASS = "com.fasterxml.jackson.dataformat.toml.TomlFactory";

    @Override
    public String name() {
        return "toml";
    }

    @Override
    public Set<String> extensions() {
        return Set.of("toml");
    }

    @Override
    public Set<String> mediaTypes() {
        return Set.of("application/toml");
    }

    @Override
    protected ObjectMapper createMapper() {
        try {
            IClass<?> factoryClass = IClass.forName(TOML_FACTORY_CLASS);
            var factory = factoryClass.getDeclaredConstructor().newInstance();
            return new ObjectMapper((com.fasterxml.jackson.core.JsonFactory) factory);
        } catch (Exception e) {
            throw new IllegalStateException("TOML support not available", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return isClassAvailable(TOML_FACTORY_CLASS);
    }
}
