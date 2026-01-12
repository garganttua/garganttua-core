package com.garganttua.core.dsl.dependency;

/**
 * Defines the lifecycle phases when a dependency is required by a builder.
 *
 * <p>
 * {@code DependencyPhase} allows fine-grained control over when dependencies
 * are needed during the builder's lifecycle. This enables builders to declare
 * dependencies that are only needed during specific phases like auto-detection
 * or build, avoiding unnecessary coupling.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Dependency needed only during auto-detection
 * DependencySpec.use(IInjectionContextBuilder.class, DependencyPhase.AUTO_DETECT)
 *
 * // Dependency needed only during build
 * DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
 *
 * // Dependency needed in both phases
 * DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BOTH)
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see DependencySpec
 * @see IDependentBuilder
 */
public enum DependencyPhase {
    /**
     * Dependency is needed only during the auto-detection phase.
     * The dependency will be processed by {@code doAutoDetectionWithDependency()}.
     */
    AUTO_DETECT,

    /**
     * Dependency is needed only during the build phase (pre-build and post-build).
     * The dependency will be processed by {@code doPreBuildWithDependency()} and
     * {@code doPostBuildWithDependency()}.
     */
    BUILD,

    /**
     * Dependency is needed in both auto-detection and build phases.
     * The dependency will be processed in all dependency handling methods.
     */
    BOTH;

    /**
     * Checks if this phase includes auto-detection.
     *
     * @return {@code true} if this phase is AUTO_DETECT or BOTH
     */
    public boolean includesAutoDetect() {
        return this == AUTO_DETECT || this == BOTH;
    }

    /**
     * Checks if this phase includes build.
     *
     * @return {@code true} if this phase is BUILD or BOTH
     */
    public boolean includesBuild() {
        return this == BUILD || this == BOTH;
    }
}
