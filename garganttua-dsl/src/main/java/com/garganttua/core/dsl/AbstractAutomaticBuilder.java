package com.garganttua.core.dsl;

import java.util.Objects;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IReflection;

/**
 * Base class for automatic builders that cache their built result and support
 * optional auto-detection (including {@code @Scan}-driven package discovery) and
 * rebuild-with-merge semantics.
 *
 * @param <Builder> the concrete builder type returned for method chaining
 * @param <Built>   the type of object this builder produces
 */
public abstract class AbstractAutomaticBuilder<Builder, Built> implements IRebuildableBuilder<Builder, Built> {
    private static final Logger log = Logger.getLogger(AbstractAutomaticBuilder.class);

    protected Boolean autoDetect;
    protected Built built;
    protected boolean invalidated = false;

    protected AbstractAutomaticBuilder() {
        log.trace("Entering AbstractAutomaticBuilder constructor");
        this.autoDetect = false;
        log.trace("Exiting AbstractAutomaticBuilder constructor, autoDetect set to false");
    }

    /**
     * Indicates whether auto-detection is enabled on this builder.
     *
     * @return {@code true} if auto-detection will run during {@link #build()}
     */
    @Override
    public boolean isAutoDetected() {
        return this.autoDetect.booleanValue();
    }

    /**
     * Enables or disables auto-detection for this builder.
     *
     * @param b {@code true} to run auto-detection during {@link #build()}
     * @return this builder for chaining
     * @throws DslException if the flag cannot be set
     */
    @SuppressWarnings("unchecked")
    @Override
    public Builder autoDetect(boolean b) throws DslException {
        log.trace("Entering autoDetect with value: {}", b);
        try {
            this.autoDetect = Objects.requireNonNull(b, "AutoDetect cannot be null");
            log.debug("AutoDetect set to: {}", this.autoDetect);
            log.trace("Exiting autoDetect");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.error("AutoDetect parameter cannot be null", e);
            throw new DslException("AutoDetect parameter cannot be null", e);
        }
    }

    /**
     * Builds and caches the target object. Returns the cached instance on
     * subsequent calls. When auto-detection is enabled, runs {@code @Scan}
     * package discovery (for {@link IPackageableBuilder}s) followed by
     * {@link #doAutoDetection()} before delegating to {@link #doBuild()}.
     *
     * @return the built (and cached) instance
     * @throws DslException if auto-detection or building fails
     */
    @Override
    public Built build() throws DslException {
        log.trace("Entering build method");

        if (this.built != null) {
            log.debug("Returning previously built instance: {}", this.built);
            log.trace("Exiting build method (cached)");
            return this.built;
        }

        if (this.autoDetect.booleanValue()) {
            log.debug("Auto-detection is enabled, performing auto-detection");

            // Scan for @Scan annotations if builder is packageable and has packages
            if (this instanceof IPackageableBuilder) {
                IReflection reflect = getReflection();
                String[] packages = getPackagesForScanning();
                if (reflect != null && packages != null && packages.length > 0) {
                    log.debug("Scanning {} packages for @Scan annotations before business auto-detection", packages.length);
                    new PackageScanHelper(reflect).scanAndAddPackages(this, packages);
                }
            }

            this.doAutoDetection();
            log.debug("Auto-detection completed successfully");
        } else {
            log.debug("Auto-detection is disabled, skipping auto-detection");
        }

        try {
            log.debug("Building the instance");
            this.built = this.doBuild();
            log.debug("Built instance: {}", this.built);
            log.trace("Exiting build method");
            return this.built;
        } catch (DslException e) {
            // Propagate; top-level callers decide the severity. See sibling
            // builders for the same rationale.
            log.debug("Build failed, propagating to caller: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Creates the target object. Invoked by {@link #build()} and
     * {@link #rebuild()} after any auto-detection has run.
     *
     * @return the newly built instance
     * @throws DslException if building fails
     */
    protected abstract Built doBuild() throws DslException;

    /**
     * Performs builder-specific auto-detection. Invoked only when
     * auto-detection is enabled.
     *
     * @throws DslException if auto-detection fails
     */
    protected abstract void doAutoDetection() throws DslException;

    /**
     * Marks this builder as invalidated so the next {@link #rebuild()} discards
     * the cached instance and rebuilds.
     *
     * @return this builder for chaining
     */
    @SuppressWarnings("unchecked")
    @Override
    public Builder invalidate() {
        log.trace("Entering invalidate()");
        this.invalidated = true;
        log.debug("Builder invalidated, will rebuild on next rebuild() call");
        log.trace("Exiting invalidate()");
        return (Builder) this;
    }

    /**
     * Indicates whether this builder has been invalidated since the last build.
     *
     * @return {@code true} if {@link #invalidate()} was called
     */
    @Override
    public boolean isInvalidated() {
        return this.invalidated;
    }

    /**
     * Clears the cache, re-runs auto-detection, rebuilds, and merges the new
     * instance with the previously built one via {@link #doMerge(Object, Object)}.
     *
     * @return the rebuilt (and merged) instance
     * @throws DslException if auto-detection or building fails
     */
    @Override
    public Built rebuild() throws DslException {
        log.trace("Entering rebuild()");

        Built previouslyBuilt = this.built;
        log.debug("Stored reference to previously built instance: {}", previouslyBuilt);

        // Clear cache
        this.built = null;
        this.invalidated = false;
        log.debug("Cleared cached instance and invalidation flag");

        // Re-run auto-detection (discovers new components)
        if (this.autoDetect.booleanValue()) {
            log.debug("Auto-detection is enabled, performing auto-detection during rebuild");

            // Scan for @Scan annotations if builder is packageable and has packages
            if (this instanceof IPackageableBuilder) {
                IReflection reflect = getReflection();
                String[] packages = getPackagesForScanning();
                if (reflect != null && packages != null && packages.length > 0) {
                    log.debug("Scanning {} packages for @Scan annotations during rebuild", packages.length);
                    new PackageScanHelper(reflect).scanAndAddPackages(this, packages);
                }
            }

            this.doAutoDetection();
            log.debug("Auto-detection completed during rebuild");
        }

        // Rebuild
        try {
            log.debug("Rebuilding the instance");
            this.built = this.doBuild();
            log.debug("Rebuilt instance: {}", this.built);
        } catch (DslException e) {
            log.debug("Rebuild failed, propagating to caller: {}", e.getMessage());
            throw e;
        }

        // Merge with previous
        if (previouslyBuilt != null) {
            log.debug("Merging previously built instance with new instance");
            this.doMerge(previouslyBuilt, this.built);
            log.debug("Merge completed");
        }

        log.trace("Exiting rebuild()");
        return this.built;
    }

    /**
     * Hook for subclasses to merge old and new built objects during rebuild.
     *
     * <p>
     * This method is called after a successful rebuild when there was a previously
     * built instance. Subclasses can override this method to implement custom
     * merge logic, such as transferring state or combining collections.
     * </p>
     *
     * <p>
     * The default implementation does nothing (no-op).
     * </p>
     *
     * @param previous the previously built instance (never null when called)
     * @param current  the newly built instance (never null when called)
     */
    protected void doMerge(Built previous, Built current) {
        // Default: no-op - subclasses can override to implement custom merge logic
        log.trace("doMerge() called - default no-op implementation");
    }

    /**
     * Returns the packages to scan for @Scan annotations.
     *
     * <p>
     * This method should be overridden by subclasses that implement IPackageableBuilder
     * to return their configured packages. The default implementation returns an empty array.
     * </p>
     *
     * @return array of package names to scan, or empty array if not applicable
     */
    protected String[] getPackagesForScanning() {
        return new String[0];
    }

    /**
     * Returns the {@link IReflection} instance used for {@code @Scan} package
     * discovery. Overridden by packageable builders; the default returns
     * {@code null} to disable scanning.
     *
     * @return the reflection facade, or {@code null} if none is available
     */
    protected IReflection getReflection() {
        return null;
    }
}