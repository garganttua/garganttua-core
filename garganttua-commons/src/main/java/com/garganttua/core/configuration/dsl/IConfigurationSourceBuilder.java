package com.garganttua.core.configuration.dsl;

import java.io.InputStream;
import java.nio.file.Path;

import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.dsl.ILinkedBuilder;

/**
 * Fluent builder for a single configuration source, selecting its origin (file,
 * classpath resource, stream, or inline content) and optional explicit format.
 *
 * <p>
 * Returns to the owning {@link IConfigurationBuilder} via {@code up()}.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IConfigurationSourceBuilder extends ILinkedBuilder<IConfigurationBuilder, Void> {

    /**
     * Reads configuration from a filesystem path.
     *
     * @param path the file path
     * @return this builder, for chaining
     */
    IConfigurationSourceBuilder file(Path path);

    /**
     * Reads configuration from a filesystem path.
     *
     * @param path the file path
     * @return this builder, for chaining
     */
    IConfigurationSourceBuilder file(String path);

    /**
     * Reads configuration from a classpath resource.
     *
     * @param resource the resource path on the classpath
     * @return this builder, for chaining
     */
    IConfigurationSourceBuilder classpath(String resource);

    /**
     * Reads configuration from the given input stream.
     *
     * @param stream the content stream
     * @return this builder, for chaining
     */
    IConfigurationSourceBuilder stream(InputStream stream);

    /**
     * Uses the given string as inline configuration content.
     *
     * @param content the raw configuration text
     * @return this builder, for chaining
     */
    IConfigurationSourceBuilder inline(String content);

    /**
     * Forces a specific format for this source instead of auto-detecting it.
     *
     * @param format the format to parse this source with
     * @return this builder, for chaining
     */
    IConfigurationSourceBuilder format(IConfigurationFormat format);
}
