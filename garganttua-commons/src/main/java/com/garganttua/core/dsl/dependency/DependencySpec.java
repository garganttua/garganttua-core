package com.garganttua.core.dsl.dependency;

import java.util.Objects;

import com.garganttua.core.dsl.IObservableBuilder;

/**
 * Specification of a dependency with its lifecycle phase and requirement level.
 *
 * <p>
 * {@code DependencySpec} combines a dependency class with the phase(s) during
 * which it is needed and whether it's required or optional in each phase.
 * This allows builders to declare fine-grained dependency requirements.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * 
 * <pre>{@code
 * // Simple cases - same requirement in all phases
 * DependencySpec.use(IConfigBuilder.class, DependencyPhase.AUTO_DETECT)
 * DependencySpec.require(IInjectionContextBuilder.class, DependencyPhase.BUILD)
 *
 * // Advanced - different requirements per phase
 * DependencySpec.of(IConfigBuilder.class)
 *     .requireForAutoDetect()
 *     .useForBuild()
 *
 * DependencySpec.of(IInjectionContextBuilder.class)
 *     .useForAutoDetect()
 *     .requireForBuild()
 * }</pre>
 *
 * @param dependencyBuilderClass the class of the dependency builder
 * @param phase           the lifecycle phase when this dependency is needed
 * @param requirement     the requirement level (optional/required per phase)
 * @since 2.0.0-ALPHA01
 * @see DependencyPhase
 * @see DependencyRequirement
 * @see IDependentBuilder
 */
public record DependencySpec(
        Class<? extends IObservableBuilder<?, ?>> dependencyBuilderClass,
        DependencyPhase phase,
        DependencyRequirement requirement) {

    /**
     * Constructs a new DependencySpec with validation.
     *
     * @param dependencyBuilderClass the class of the dependency builder
     * @param phase           the lifecycle phase when this dependency is needed
     * @param requirement     the requirement level
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if phase-specific requirement doesn't match
     *                                  phase
     */
    public DependencySpec {
        Objects.requireNonNull(dependencyBuilderClass, "Dependency builder class cannot be null");
        Objects.requireNonNull(phase, "Dependency phase cannot be null");
        Objects.requireNonNull(requirement, "Dependency requirement cannot be null");

        // Validate that phase-specific requirements are only used with BOTH phase
        if ((requirement == DependencyRequirement.REQUIRED_FOR_AUTO_DETECT ||
                requirement == DependencyRequirement.REQUIRED_FOR_BUILD) &&
                phase != DependencyPhase.BOTH) {
            throw new IllegalArgumentException(
                    "Phase-specific requirements (REQUIRED_FOR_AUTO_DETECT, REQUIRED_FOR_BUILD) " +
                            "can only be used with DependencyPhase.BOTH, got: " + phase);
        }
    }

    /**
     * Deprecated constructor for backward compatibility.
     *
     * @param dependencyClass the class of the dependency builder
     * @param phase           the lifecycle phase when this dependency is needed
     * @param required        whether this dependency is mandatory
     * @deprecated Use
     *             {@link #DependencySpec(Class, DependencyPhase, DependencyRequirement)}
     *             instead
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    public DependencySpec(
            Class<? extends IObservableBuilder<?, ?>> dependencyClass,
            DependencyPhase phase,
            boolean required) {
        this(dependencyClass, phase,
                required ? DependencyRequirement.REQUIRED : DependencyRequirement.OPTIONAL);
    }

    /**
     * Creates an optional dependency specification.
     *
     * @param dependencyClass the class of the dependency builder
     * @param phase           the lifecycle phase when this dependency is needed
     * @return a new optional DependencySpec
     */
    public static DependencySpec use(
            Class<? extends IObservableBuilder<?, ?>> dependencyClass,
            DependencyPhase phase) {
        return new DependencySpec(dependencyClass, phase, DependencyRequirement.OPTIONAL);
    }

    /**
     * Creates a required dependency specification.
     *
     * @param dependencyClass the class of the dependency builder
     * @param phase           the lifecycle phase when this dependency is needed
     * @return a new required DependencySpec
     */
    public static DependencySpec require(
            Class<? extends IObservableBuilder<?, ?>> dependencyClass,
            DependencyPhase phase) {
        return new DependencySpec(dependencyClass, phase, DependencyRequirement.REQUIRED);
    }

    /**
     * Creates an optional dependency specification for both phases.
     *
     * @param dependencyClass the class of the dependency builder
     * @return a new optional DependencySpec for both phases
     */
    public static DependencySpec use(Class<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass, DependencyPhase.BOTH, DependencyRequirement.OPTIONAL);
    }

    /**
     * Creates a required dependency specification for both phases.
     *
     * @param dependencyClass the class of the dependency builder
     * @return a new required DependencySpec for both phases
     */
    public static DependencySpec require(Class<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass, DependencyPhase.BOTH, DependencyRequirement.REQUIRED);
    }

    /**
     * Creates a builder for phase-specific dependency requirements.
     *
     * @param dependencyClass the class of the dependency builder
     * @return a new DependencySpecBuilder for fine-grained configuration
     */
    public static DependencySpecBuilder of(Class<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpecBuilder(dependencyClass);
    }

    /**
     * Checks if this dependency is needed during auto-detection phase.
     *
     * @return {@code true} if needed during auto-detection
     */
    public boolean isNeededForAutoDetect() {
        return phase.includesAutoDetect();
    }

    /**
     * Checks if this dependency is needed during build phase.
     *
     * @return {@code true} if needed during build
     */
    public boolean isNeededForBuild() {
        return phase.includesBuild();
    }

    /**
     * Checks if this dependency is required during auto-detection phase.
     *
     * @return {@code true} if required during auto-detection
     */
    public boolean isRequiredForAutoDetect() {
        return requirement.isRequiredForAutoDetect(phase);
    }

    /**
     * Checks if this dependency is required during build phase.
     *
     * @return {@code true} if required during build
     */
    public boolean isRequiredForBuild() {
        return requirement.isRequiredForBuild(phase);
    }

    /**
     * Checks if this dependency is optional during auto-detection phase.
     *
     * @return {@code true} if optional during auto-detection
     */
    public boolean isOptionalForAutoDetect() {
        return requirement.isOptionalForAutoDetect(phase);
    }

    /**
     * Checks if this dependency is optional during build phase.
     *
     * @return {@code true} if optional during build
     */
    public boolean isOptionalForBuild() {
        return requirement.isOptionalForBuild(phase);
    }

    /**
     * Legacy method for backward compatibility.
     *
     * @return true if requirement is REQUIRED or phase-specific required
     * @deprecated Use {@link #isRequiredForAutoDetect()} or
     *             {@link #isRequiredForBuild()} instead
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    public boolean required() {
        return requirement == DependencyRequirement.REQUIRED ||
                requirement == DependencyRequirement.REQUIRED_FOR_AUTO_DETECT ||
                requirement == DependencyRequirement.REQUIRED_FOR_BUILD;
    }
}
