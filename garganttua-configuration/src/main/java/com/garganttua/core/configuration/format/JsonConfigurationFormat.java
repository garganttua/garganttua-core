package com.garganttua.core.configuration.format;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonConfigurationFormat extends AbstractConfigurationFormat {

    @Override
    public String name() {
        return "json";
    }

    @Override
    public Set<String> extensions() {
        return Set.of("json");
    }

    @Override
    public Set<String> mediaTypes() {
        return Set.of("application/json");
    }

    @Override
    protected ObjectMapper createMapper() {
        return new ObjectMapper();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
