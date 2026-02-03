package com.garganttua.core.dsl;

import java.util.Objects;

import com.garganttua.core.reflection.IAnnotationScanner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractAutomaticBuilder<Builder, Built> implements IRebuildableBuilder<Builder, Built> {

    protected Boolean autoDetect;
    protected Built built;
    protected boolean invalidated = false;

    protected AbstractAutomaticBuilder() {
        log.atTrace().log("Entering AbstractAutomaticBuilder constructor");
        this.autoDetect = false;
        log.atTrace().log("Exiting AbstractAutomaticBuilder constructor, autoDetect set to false");
    }

    @Override
    public boolean isAutoDetected() {
        return this.autoDetect.booleanValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder autoDetect(boolean b) throws DslException {
        log.atTrace().log("Entering autoDetect with value: {}", b);
        try {
            this.autoDetect = Objects.requireNonNull(b, "AutoDetect cannot be null");
            log.atDebug().log("AutoDetect set to: {}", this.autoDetect);
            log.atTrace().log("Exiting autoDetect");
            return (Builder) this;
        } catch (NullPointerException e) {
            log.atError().log("AutoDetect parameter cannot be null", e);
            throw new DslException("AutoDetect parameter cannot be null", e);
        }
    }

    @Override
    public Built build() throws DslException {
        log.atTrace().log("Entering build method");

        if (this.built != null) {
            log.atDebug().log("Returning previously built instance: {}", this.built);
            log.atTrace().log("Exiting build method (cached)");
            return this.built;
        }

        if (this.autoDetect.booleanValue()) {
            log.atDebug().log("Auto-detection is enabled, performing auto-detection");

            // Scan for @Scan annotations if builder is packageable and has packages
            if (this instanceof IPackageableBuilder) {
                IAnnotationScanner scanner = getAnnotationScanner();
                String[] packages = getPackagesForScanning();
                if (scanner != null && packages != null && packages.length > 0) {
                    log.atDebug().log("Scanning {} packages for @Scan annotations before business auto-detection", packages.length);
                    PackageScanHelper.scanAndAddPackages(scanner, this, packages);
                }
            }

            this.doAutoDetection();
            log.atDebug().log("Auto-detection completed successfully");
        } else {
            log.atDebug().log("Auto-detection is disabled, skipping auto-detection");
        }

        try {
            log.atDebug().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);
            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
            throw e;
        }
    }

    protected abstract Built doBuild() throws DslException;

    protected abstract void doAutoDetection() throws DslException;

    @SuppressWarnings("unchecked")
    @Override
    public Builder invalidate() {
        log.atTrace().log("Entering invalidate()");
        this.invalidated = true;
        log.atDebug().log("Builder invalidated, will rebuild on next rebuild() call");
        log.atTrace().log("Exiting invalidate()");
        return (Builder) this;
    }

    @Override
    public boolean isInvalidated() {
        return this.invalidated;
    }

    @Override
    public Built rebuild() throws DslException {
        log.atTrace().log("Entering rebuild()");

        Built previouslyBuilt = this.built;
        log.atDebug().log("Stored reference to previously built instance: {}", previouslyBuilt);

        // Clear cache
        this.built = null;
        this.invalidated = false;
        log.atDebug().log("Cleared cached instance and invalidation flag");

        // Re-run auto-detection (discovers new components)
        if (this.autoDetect.booleanValue()) {
            log.atDebug().log("Auto-detection is enabled, performing auto-detection during rebuild");

            // Scan for @Scan annotations if builder is packageable and has packages
            if (this instanceof IPackageableBuilder) {
                IAnnotationScanner scanner = getAnnotationScanner();
                String[] packages = getPackagesForScanning();
                if (scanner != null && packages != null && packages.length > 0) {
                    log.atDebug().log("Scanning {} packages for @Scan annotations during rebuild", packages.length);
                    PackageScanHelper.scanAndAddPackages(scanner, this, packages);
                }
            }

            this.doAutoDetection();
            log.atDebug().log("Auto-detection completed during rebuild");
        }

        // Rebuild
        try {
            log.atDebug().log("Rebuilding the instance");
            this.built = this.doBuild();
            log.atDebug().log("Rebuilt instance: {}", this.built);
        } catch (DslException e) {
            log.atError().log("Critical error during rebuild", e);
            throw e;
        }

        // Merge with previous
        if (previouslyBuilt != null) {
            log.atDebug().log("Merging previously built instance with new instance");
            this.doMerge(previouslyBuilt, this.built);
            log.atDebug().log("Merge completed");
        }

        log.atTrace().log("Exiting rebuild()");
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
        log.atTrace().log("doMerge() called - default no-op implementation");
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
     * Returns the annotation scanner to use for scanning @Scan annotations.
     *
     * <p>
     * This method should be overridden by subclasses that want to enable @Scan annotation
     * scanning during auto-detection. The default implementation returns null, which disables
     * @Scan scanning.
     * </p>
     *
     * @return the annotation scanner to use, or null to disable @Scan scanning
     */
    protected IAnnotationScanner getAnnotationScanner() {
        return null;
    }
}