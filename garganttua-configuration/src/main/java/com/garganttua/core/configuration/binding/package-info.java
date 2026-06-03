/**
 * Binds external configuration files to DSL builders.
 *
 * <p>A configuration file declares which builder it targets via a short <em>alias</em>
 * (shebang) — {@code #!injection} for text formats, {@code <?garganttua module="injection"?>}
 * for XML, the reserved {@code "$module":"injection"} key for JSON — read by
 * {@link com.garganttua.core.configuration.binding.ConfigurationShebang}. The alias maps to
 * the builder annotated {@code @ConfigurableBuilder("injection")}
 * ({@link com.garganttua.core.configuration.binding.ConfigurableBuilderRegistry}), and
 * {@link com.garganttua.core.configuration.binding.ConfigurationApplier} populates that
 * builder from the file (scalars and nested child-builders), ignoring the reserved
 * {@code $}-prefixed metadata keys.</p>
 *
 * <p>{@link com.garganttua.core.configuration.binding.BootstrapConfigurationContributor}
 * wires this into the bootstrap: discovered via the
 * {@code IBootstrapConfigurationContributor} SPI, it runs at the {@code CONFIGURATION}
 * stage and applies discovered files to the matching {@code @ConfigurableBuilder}s before
 * they build — active only when this module is on the classpath (optional).</p>
 *
 * @since 2.0.0-ALPHA02
 */
package com.garganttua.core.configuration.binding;
