package com.garganttua.core.configuration;

import java.io.InputStream;
import java.util.Set;

/**
 * A pluggable configuration format (JSON, YAML, XML, TOML, properties, ...)
 * able to parse raw input into a {@link IConfigurationNode} tree.
 *
 * <p>
 * Implementations advertise the file extensions and media types they handle and
 * may report themselves unavailable when their backing library is missing from
 * the classpath.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IConfigurationFormat {

    /**
     * @return the human-readable name of this format (e.g. {@code "json"})
     */
    String name();

    /**
     * @return the file extensions this format handles, without leading dot
     */
    Set<String> extensions();

    /**
     * @return the media types (MIME types) this format handles
     */
    Set<String> mediaTypes();

    /**
     * Parses the given input stream into a configuration node tree.
     *
     * @param input the raw configuration content; not closed by this method
     * @return the parsed root node
     * @throws ConfigurationException if the content cannot be parsed
     */
    IConfigurationNode parse(InputStream input) throws ConfigurationException;

    /**
     * Tests whether this format handles the given extension or media type.
     *
     * @param extensionOrMediaType a file extension or media type
     * @return {@code true} if supported by this format
     */
    boolean supports(String extensionOrMediaType);

    /**
     * @return {@code true} if the backing parser is present and usable on the
     *         current classpath
     */
    boolean isAvailable();
}
