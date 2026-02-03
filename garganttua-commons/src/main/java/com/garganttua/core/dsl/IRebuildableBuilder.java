package com.garganttua.core.dsl;

/**
 * Builder interface that supports invalidation and rebuilding of cached instances.
 *
 * <p>
 * {@code IRebuildableBuilder} extends {@link IAutomaticBuilder} to provide the ability
 * to invalidate a previously built instance and rebuild it with potentially new
 * configurations or detected components. This is particularly useful for hot-reload
 * scenarios where new components (e.g., from dynamically loaded JARs) need to be
 * integrated into an existing application.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Initial build
 * IRebuildableBuilder<Builder, Context> builder = new ContextBuilder()
 *         .withPackage("com.myapp")
 *         .autoDetect(true);
 * Context context = builder.build();
 *
 * // Later, after loading a new JAR...
 * builder.withPackage("com.plugin");
 * Context updatedContext = builder.rebuild();
 * }</pre>
 *
 * <h2>Rebuild Process</h2>
 * <p>
 * The rebuild process typically follows these steps:
 * </p>
 * <ol>
 *   <li>Save reference to the previously built instance</li>
 *   <li>Clear the cached instance</li>
 *   <li>Re-run auto-detection (if enabled) to discover new components</li>
 *   <li>Build a new instance</li>
 *   <li>Merge the old and new instances via {@code doMerge()}</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations are typically not thread-safe. Rebuild operations should be
 * performed from a single thread, and the application should ensure no concurrent
 * access to the built objects during rebuild.
 * </p>
 *
 * @param <Builder> the concrete builder type for method chaining
 * @param <Built>   the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see IAutomaticBuilder
 */
public interface IRebuildableBuilder<Builder, Built> extends IAutomaticBuilder<Builder, Built> {

    /**
     * Invalidates the cached built instance, allowing a subsequent rebuild.
     *
     * <p>
     * This method marks the builder as invalidated without actually clearing
     * the cached instance. Use {@link #rebuild()} to perform the actual rebuild.
     * </p>
     *
     * @return this builder instance for method chaining
     */
    Builder invalidate();

    /**
     * Rebuilds the instance by invalidating the cache, re-running auto-detection,
     * building a new instance, and optionally merging with the previous instance.
     *
     * <p>
     * This method performs a full rebuild cycle:
     * </p>
     * <ol>
     *   <li>Saves reference to the previously built instance</li>
     *   <li>Clears the cached instance and invalidation flag</li>
     *   <li>Re-runs auto-detection if enabled</li>
     *   <li>Builds a new instance via {@code doBuild()}</li>
     *   <li>Calls {@code doMerge()} to merge previous and new instances</li>
     * </ol>
     *
     * @return the newly built instance
     * @throws DslException if the rebuild fails
     */
    Built rebuild() throws DslException;

    /**
     * Checks whether this builder has been invalidated.
     *
     * <p>
     * An invalidated builder will perform a full rebuild on the next call to
     * {@link #rebuild()} instead of returning the cached instance.
     * </p>
     *
     * @return {@code true} if the builder has been invalidated, {@code false} otherwise
     */
    boolean isInvalidated();

}
