/**
 * Configuration contracts defining sources, formats, nodes, and populator interfaces
 * for multi-format configuration loading.
 *
 * <p>
 * {@link com.garganttua.core.configuration.IConfigurationSource} supplies raw
 * content, {@link com.garganttua.core.configuration.IConfigurationFormat} parses
 * it into a {@link com.garganttua.core.configuration.IConfigurationNode} tree,
 * and {@link com.garganttua.core.configuration.IConfigurationPopulator} maps that
 * tree onto fluent builders.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
package com.garganttua.core.configuration;
