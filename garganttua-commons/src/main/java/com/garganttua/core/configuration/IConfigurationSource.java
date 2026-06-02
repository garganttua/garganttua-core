package com.garganttua.core.configuration;

import java.io.InputStream;
import java.util.Optional;

/**
 * A source of raw configuration content (file, classpath resource, stream,
 * inline string, ...) that can be opened for reading.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IConfigurationSource {

    /**
     * Opens a fresh stream over this source's content.
     *
     * @return the content stream; the caller is responsible for closing it
     * @throws ConfigurationException if the source cannot be opened
     */
    InputStream getInputStream() throws ConfigurationException;

    /**
     * @return a hint identifying the content format (e.g. file extension or
     *         media type), or empty if unknown
     */
    Optional<String> getFormatHint();

    /**
     * @return a human-readable description of this source, for diagnostics
     */
    String getDescription();
}
