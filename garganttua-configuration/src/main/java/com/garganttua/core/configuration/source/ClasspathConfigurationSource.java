package com.garganttua.core.configuration.source;

import java.io.InputStream;
import java.util.Optional;

import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationSource;

/**
 * {@link IConfigurationSource} that loads configuration from a classpath resource,
 * deriving the format hint from the resource file extension.
 */
public class ClasspathConfigurationSource implements IConfigurationSource {

    private final String resource;

    /**
     * Creates a source backed by a classpath resource.
     *
     * @param resource the classpath resource path
     */
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
