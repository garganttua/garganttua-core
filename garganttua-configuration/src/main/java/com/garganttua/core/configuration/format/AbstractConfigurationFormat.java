package com.garganttua.core.configuration.format;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.configuration.ConfigurationException;
import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationNode;
import com.garganttua.core.configuration.node.ConfigurationNode;
import com.garganttua.core.reflection.IClass;

/**
 * Base {@link IConfigurationFormat} implementation backed by a Jackson {@link ObjectMapper}.
 * Subclasses provide a format-specific mapper and the format name, extensions, and media types.
 */
public abstract class AbstractConfigurationFormat implements IConfigurationFormat {
    private static final Logger log = Logger.getLogger(AbstractConfigurationFormat.class);

    /**
     * @return a Jackson {@link ObjectMapper} configured for this format
     */
    protected abstract ObjectMapper createMapper();

    /**
     * Parses the input stream into a configuration tree using this format's mapper.
     *
     * @param input the configuration content to parse
     * @return the parsed configuration node tree
     * @throws ConfigurationException if the input cannot be read or parsed
     */
    @Override
    public IConfigurationNode parse(InputStream input) throws ConfigurationException {
        log.debug("Parsing configuration with format: {}", name());
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
        return extensions().contains(extensionOrMediaType.toLowerCase(java.util.Locale.ROOT))
                || mediaTypes().contains(extensionOrMediaType.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * Tests whether a class is resolvable on the classpath, used to gate optional format support.
     *
     * @param className the fully qualified class name to probe
     * @return {@code true} if the class can be loaded, {@code false} otherwise
     */
    protected static boolean isClassAvailable(String className) {
        try {
            IClass.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
