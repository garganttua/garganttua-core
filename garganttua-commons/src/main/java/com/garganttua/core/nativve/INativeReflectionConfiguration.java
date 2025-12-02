package com.garganttua.core.nativve;

import java.util.Set;

/**
 * Interface for providing native image reflection configuration entries.
 *
 * <p>
 * {@code INativeConfiguration} is implemented by configuration classes that provide
 * a set of reflection configuration entries for GraalVM native image builds. This
 * interface enables automatic collection of reflection metadata required for proper
 * runtime operation in native executables.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * public class MyNativeConfiguration implements INativeConfiguration {
 *     @Override
 *     public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
 *         return Set.of(
 *             ReflectionConfigEntryBuilder.forClass(MyService.class)
 *                 .queryAllDeclaredConstructors(true)
 *                 .queryAllDeclaredMethods(true),
 *             ReflectionConfigEntryBuilder.forClass(MyRepository.class)
 *                 .allDeclaredFields(true)
 *         );
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionConfigurationEntryBuilder
 * @see INativeElement
 */
public interface INativeReflectionConfiguration {

    /**
     * Returns the set of reflection configuration entry builders for native image.
     *
     * <p>
     * This method provides the collection of reflection configuration entries that
     * specify which classes, methods, fields, and constructors should be registered
     * for reflection access in GraalVM native images. The framework aggregates these
     * configurations during the build process to generate the reflect-config.json file.
     * </p>
     *
     * @return a set of reflection configuration entry builders, never {@code null}
     */
    Set<IReflectionConfigurationEntryBuilder> nativeConfiguration();

}
