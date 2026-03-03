package com.garganttua.core.configuration.source;

import java.io.InputStream;
import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

public class ClasspathConfigurationSource implements IConfigurationSource {

    private final String resource;

    public ClasspathConfigurationSource(String resource) {
        this.resource = resource;
    }

    @Override
    public InputStream getInputStream() throws ConfigurationException {
        var is = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.resource);
        if (is == null) {
            is = getClass().getClassLoader().getResourceAsStream(this.resource);
        }
        if (is == null) {
            throw new ConfigurationException("Classpath resource not found: " + this.resource);
        }
        return is;
    }

    @Override
    public Optional<String> getFormatHint() {
        int dot = this.resource.lastIndexOf('.');
        if (dot > 0) {
            return Optional.of(this.resource.substring(dot + 1));
        }
        return Optional.empty();
    }

    @Override
    public String getDescription() {
        return "classpath:" + this.resource;
    }
}
