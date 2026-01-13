package com.garganttua.core.dsl;

/**
 * Builder interface that supports automatic configuration detection.
 *
 * <p>
 * {@code IAutomaticBuilder} extends {@link IBuilder} to provide automatic
 * configuration
 * capabilities. When enabled, the builder can detect and apply configurations
 * from
 * various sources (classpath, annotations, conventions) without explicit manual
 * setup.
 * This reduces boilerplate code while maintaining flexibility for manual
 * override.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * // With automatic detection enabled
 * ServiceConfig config = new ServiceConfigBuilder()
 *         .autoDetect(true) // Scans classpath for configuration files
 *         .build();
 *
 * // With manual configuration
 * ServiceConfig config = new ServiceConfigBuilder()
 *         .autoDetect(false)
 *         .property("host", "localhost")
 *         .property("port", 8080)
 *         .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations are typically not thread-safe. Each builder instance should
 * be
 * used by a single thread during construction.
 * </p>
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Built>   the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 * @see IAutomaticLinkedBuilder
 */
public interface IAutomaticBuilder<Builder, Built> extends IBuilder<Built> {

    /**
     * Enables or disables automatic configuration detection.
     *
     * <p>
     * When enabled, the builder will attempt to automatically detect and apply
     * configurations from available sources such as classpath resources,
     * annotations,
     * or convention-based locations. When disabled, all configuration must be
     * explicitly provided through builder methods.
     * </p>
     *
     * @param b {@code true} to enable automatic detection, {@code false} to disable
     * @return this builder instance for method chaining
     * @throws DslException if automatic detection fails or encounters invalid
     *                      configuration
     */
    Builder autoDetect(boolean b) throws DslException;

    boolean isAutoDetected();

}
