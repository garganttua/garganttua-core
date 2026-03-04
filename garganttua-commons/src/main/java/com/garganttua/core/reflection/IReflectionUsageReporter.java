package com.garganttua.core.reflection;

import java.util.Set;

import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;

/**
 * Interface for reporting reflection usage.
 *
 * <p>
 * {@code IReflectionUsageReporter} is implemented by classes that programmatically
 * declare which elements require reflective access at runtime. This interface enables
 * automatic collection of reflection metadata for both AOT compilation (e.g., GraalVM
 * native images) and runtime analysis.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * </p>
 * <pre>{@code
 * public class MyReflectionConfig implements IReflectionUsageReporter {
 *     @Override
 *     public Set<IReflectionConfigurationEntryBuilder> reflectionUsage() {
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
 */
public interface IReflectionUsageReporter {

    /**
     * Returns the set of reflection configuration entry builders declaring reflection usage.
     *
     * <p>
     * This method provides the collection of reflection configuration entries that
     * specify which classes, methods, fields, and constructors require reflective
     * access at runtime.
     * </p>
     *
     * @return a set of reflection configuration entry builders, never {@code null}
     */
    Set<IReflectionConfigurationEntryBuilder> reflectionUsage();

}
