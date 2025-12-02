package com.garganttua.core.nativve;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;

/**
 * Builder interface for constructing native image configuration objects.
 *
 * <p>
 * {@code INativeBuilder} combines package-based scanning capabilities with automatic
 * building to create native image configurations. This builder enables fluent construction
 * of reflection configuration entries for GraalVM native image compilation.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * INativeConfiguration config = nativeBuilder
 *     .scanPackages("com.example.service", "com.example.repository")
 *     .build();
 *
 * // Access generated configuration entries
 * Set<IReflectionConfigurationEntryBuilder> entries = config.nativeConfiguration();
 * }</pre>
 *
 * @param <B> the concrete builder type for method chaining
 * @param <C> the configuration type being built, must extend {@link INativeConfiguration}
 *
 * @since 2.0.0-ALPHA01
 * @see INativeConfiguration
 * @see IPackageableBuilder
 * @see IAutomaticBuilder
 */
public interface INativeBuilder<B, C extends INativeConfiguration> extends IPackageableBuilder<B, C>, IAutomaticBuilder<B, C>{

}
