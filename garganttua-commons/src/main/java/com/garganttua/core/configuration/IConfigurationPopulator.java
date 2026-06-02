package com.garganttua.core.configuration;

import com.garganttua.core.dsl.IBuilder;

/**
 * Applies configuration values onto a fluent {@link IBuilder} by mapping config
 * keys to builder methods, recursing into nested child builders.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IConfigurationPopulator {

    /**
     * Populates the builder from an already-parsed configuration node.
     *
     * @param builder the target builder
     * @param node    the configuration tree to apply
     * @param <B>     the builder type
     * @return the same builder, for chaining
     * @throws ConfigurationException if a value cannot be mapped onto the builder
     */
    <B extends IBuilder<?>> B populate(B builder, IConfigurationNode node) throws ConfigurationException;

    /**
     * Populates the builder from a source, detecting the format from the
     * source's format hint.
     *
     * @param builder the target builder
     * @param source  the configuration source to read and parse
     * @param <B>     the builder type
     * @return the same builder, for chaining
     * @throws ConfigurationException if the source cannot be read, parsed, or applied
     */
    <B extends IBuilder<?>> B populate(B builder, IConfigurationSource source) throws ConfigurationException;

    /**
     * Populates the builder from a source using the explicitly supplied format.
     *
     * @param builder the target builder
     * @param source  the configuration source to read
     * @param format  the format used to parse the source
     * @param <B>     the builder type
     * @return the same builder, for chaining
     * @throws ConfigurationException if the source cannot be read, parsed, or applied
     */
    <B extends IBuilder<?>> B populate(B builder, IConfigurationSource source, IConfigurationFormat format) throws ConfigurationException;
}
