package com.garganttua.core.dsl.dependency;

/**
 * Defines whether a dependency is required or optional, potentially varying by phase.
 *
 * <p>
 * {@code DependencyRequirement} allows specifying fine-grained requirements for dependencies.
 * A dependency can be required in one phase and optional in another, or have the same
 * requirement across all phases.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Required for auto-detection, optional for build
 * DependencySpec.of(IConfigBuilder.class)
 *     .requireForAutoDetect()
 *     .useForBuild()
 *
 * // Optional for auto-detection, required for build
 * DependencySpec.of(IInjectionContextBuilder.class)
 *     .useForAutoDetect()
 *     .requireForBuild()
 *
 * // Required in both phases
 * DependencySpec.require(ILoggerBuilder.class, DependencyPhase.BOTH)
 *
 * // Optional in both phases
 * DependencySpec.use(ICacheBuilder.class, DependencyPhase.BOTH)
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see DependencySpec
 * @see DependencyPhase
 */
public enum DependencyRequirement {
    /**
     * Dependency is optional in all phases where it's used.
     */
    OPTIONAL,

    /**
     * Dependency is required in all phases where it's used.
     */
    REQUIRED,

    /**
     * Dependency is required during auto-detection, optional during build.
     * Only valid when phase is BOTH.
     */
    REQUIRED_FOR_AUTO_DETECT,

    /**
     * Dependency is optional during auto-detection, required during build.
     * Only valid when phase is BOTH.
     */
    REQUIRED_FOR_BUILD;

    /**
     * Checks if this dependency is required during auto-detection phase.
     *
     * @param phase the dependency phase
     * @return true if required during auto-detection
     */
    public boolean isRequiredForAutoDetect(DependencyPhase phase) {
        return switch (this) {
            case REQUIRED -> phase.includesAutoDetect();
            case REQUIRED_FOR_AUTO_DETECT -> phase.includesAutoDetect();
            case REQUIRED_FOR_BUILD -> false;
            case OPTIONAL -> false;
        };
    }

    /**
     * Checks if this dependency is required during build phase.
     *
     * @param phase the dependency phase
     * @return true if required during build
     */
    public boolean isRequiredForBuild(DependencyPhase phase) {
        return switch (this) {
            case REQUIRED -> phase.includesBuild();
            case REQUIRED_FOR_BUILD -> phase.includesBuild();
            case REQUIRED_FOR_AUTO_DETECT -> false;
            case OPTIONAL -> false;
        };
    }

    /**
     * Checks if this dependency is optional during auto-detection phase.
     *
     * @param phase the dependency phase
     * @return true if optional during auto-detection
     */
    public boolean isOptionalForAutoDetect(DependencyPhase phase) {
        return phase.includesAutoDetect() && !isRequiredForAutoDetect(phase);
    }

    /**
     * Checks if this dependency is optional during build phase.
     *
     * @param phase the dependency phase
     * @return true if optional during build
     */
    public boolean isOptionalForBuild(DependencyPhase phase) {
        return phase.includesBuild() && !isRequiredForBuild(phase);
    }
}
