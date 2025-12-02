package com.garganttua.core.nativve;

import com.garganttua.core.injection.IBeanFactory;

/**
 * Functional interface for elements that require native image reflection configuration.
 *
 * <p>
 * {@code NativeReflectionElement} is implemented by DI framework elements that need to be
 * registered in the GraalVM native image reflection configuration. This is essential for
 * components that use reflection at runtime in native images, as GraalVM requires explicit
 * declaration of all reflectively accessed classes, methods, and fields. This interface
 * enables automatic generation of reflection configuration files during native image builds.
 * </p>
 *
 *
 * @since 2.0.0-ALPHA01
 * @see IReflectionConfigurationEntry
 * @see IBeanFactory
 */
@FunctionalInterface
public interface INativeElement {

    /**
     * Returns the native image reflection configuration for this element.
     *
     * <p>
     * This method provides the reflection configuration entry that specifies which
     * classes, methods, fields, and constructors should be registered for reflection
     * access in GraalVM native images. The framework collects these configurations
     * during the build process to generate the reflect-config.json file.
     * </p>
     *
     * @return the reflection configuration entry for this element
     */
    IReflectionConfigurationEntryBuilder nativeEntry();

}
