package com.garganttua.core.dsl;

/**
 * Builder interface combining automatic detection and hierarchical navigation capabilities.
 *
 * <p>
 * {@code IAutomaticLinkedBuilder} combines the features of {@link IAutomaticBuilder}
 * and {@link ILinkedBuilder}, enabling both automatic configuration detection and
 * navigation through builder hierarchies. This is particularly useful for building
 * complex nested structures where some parts can be auto-configured while maintaining
 * the ability to navigate the builder tree.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Building nested configuration with auto-detection
 * Application app = new ApplicationBuilder()
 *     .name("MyApp")
 *     .database()
 *         .autoDetect(true)  // Auto-detect database configuration
 *         .up()
 *     .server()
 *         .port(8080)
 *         .security()
 *             .autoDetect(true)  // Auto-detect security settings
 *             .up()
 *         .up()
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations are typically not thread-safe. Each builder instance should be
 * used by a single thread during construction.
 * </p>
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Link> the type of the parent builder to navigate back to
 * @param <Built> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see IBuilder
 * @see IAutomaticBuilder
 * @see ILinkedBuilder
 */
public interface IAutomaticLinkedBuilder<Builder, Link, Built> extends IBuilder<Built> {

    /**
     * Enables or disables automatic configuration detection.
     *
     * <p>
     * When enabled, the builder will attempt to automatically detect and apply
     * configurations from available sources such as classpath resources, annotations,
     * or convention-based locations. When disabled, all configuration must be
     * explicitly provided through builder methods.
     * </p>
     *
     * @param b {@code true} to enable automatic detection, {@code false} to disable
     * @return this builder instance for method chaining
     * @throws DslException if automatic detection fails or encounters invalid configuration
     */
    Builder autoDetect(boolean b) throws DslException;

    /**
     * Navigates back to the parent builder.
     *
     * <p>
     * This method allows fluent navigation up the builder hierarchy, enabling
     * the construction of nested object structures through method chaining.
     * </p>
     *
     * @return the parent builder instance
     */
    Link up();

    /**
     * Sets the parent builder for this linked builder.
     *
     * <p>
     * This method establishes the hierarchical relationship between this builder
     * and its parent. It is typically called internally during builder construction
     * and should not be invoked by client code.
     * </p>
     *
     * @param link the parent builder instance
     * @return this builder instance for method chaining
     */
    Builder setUp(Link link);

}
