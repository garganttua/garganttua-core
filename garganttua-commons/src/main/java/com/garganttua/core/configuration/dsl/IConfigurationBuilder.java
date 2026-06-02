package com.garganttua.core.configuration.dsl;

import com.garganttua.core.configuration.IConfigurationFormat;
import com.garganttua.core.configuration.IConfigurationPopulator;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;

/**
 * Fluent builder for an {@link IConfigurationPopulator}, configuring its sources,
 * available formats, key-to-method mapping strategy, and strictness.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IConfigurationBuilder extends IAutomaticBuilder<IConfigurationBuilder, IConfigurationPopulator>, IDependentBuilder<IConfigurationBuilder, IConfigurationPopulator> {

    /**
     * Begins declaring a configuration source.
     *
     * @return a child source builder; call {@code up()} to return here
     */
    IConfigurationSourceBuilder source();

    /**
     * Registers an additional configuration format.
     *
     * @param format the format to add
     * @return this builder, for chaining
     */
    IConfigurationBuilder withFormat(IConfigurationFormat format);

    /**
     * Sets the strategy used to map configuration keys onto builder methods.
     *
     * @param strategy the strategy name (e.g. {@code SMART}, {@code DIRECT},
     *                 {@code CAMEL_CASE}, {@code KEBAB_CASE})
     * @return this builder, for chaining
     */
    IConfigurationBuilder withMappingStrategy(String strategy);

    /**
     * Enables or disables strict mode, where unknown configuration keys cause a
     * failure rather than being ignored.
     *
     * @param strict {@code true} to fail on unknown keys
     * @return this builder, for chaining
     * @throws DslException if the setting cannot be applied
     */
    IConfigurationBuilder strict(boolean strict) throws DslException;
}
