package com.garganttua.core.configuration.format;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationNode;
import com.garganttua.core.configuration.node.ConfigurationNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConfigurationFormat implements IConfigurationFormat {

    protected abstract ObjectMapper createMapper();

    @Override
    public IConfigurationNode parse(InputStream input) throws ConfigurationException {
        log.atDebug().log("Parsing configuration with format: {}", name());
        try {
            ObjectMapper mapper = createMapper();
            JsonNode tree = mapper.readTree(input);
            return new ConfigurationNode(tree);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse " + name() + " configuration", e);
        }
    }

    @Override
    public boolean supports(String extensionOrMediaType) {
        return extensions().contains(extensionOrMediaType.toLowerCase())
                || mediaTypes().contains(extensionOrMediaType.toLowerCase());
    }

    protected static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
