package com.garganttua.core.configuration.format;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.reflection.IClass;

public class YamlConfigurationFormat extends AbstractConfigurationFormat {

    private static final String YAML_FACTORY_CLASS = "com.fasterxml.jackson.dataformat.yaml.YAMLFactory";

    @Override
    public String name() {
        return "yaml";
    }

    @Override
    public Set<String> extensions() {
        return Set.of("yaml", "yml");
    }

    @Override
    public Set<String> mediaTypes() {
        return Set.of("application/yaml", "application/x-yaml", "text/yaml");
    }

    @Override
    protected ObjectMapper createMapper() {
        try {
            IClass<?> factoryClass = IClass.forName(YAML_FACTORY_CLASS);
            var factory = factoryClass.getDeclaredConstructor().newInstance();
            return new ObjectMapper((com.fasterxml.jackson.core.JsonFactory) factory);
        } catch (Exception e) {
            throw new IllegalStateException("YAML support not available", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return isClassAvailable(YAML_FACTORY_CLASS);
    }
}
