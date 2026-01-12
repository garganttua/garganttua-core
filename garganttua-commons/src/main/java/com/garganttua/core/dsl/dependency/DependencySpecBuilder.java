package com.garganttua.core.dsl.dependency;

import java.util.Objects;

import com.garganttua.core.dsl.IObservableBuilder;

/**
 * Fluent builder for creating DependencySpec with phase-specific requirements.
 *
 * <p>
 * This builder allows specifying different requirement levels (required/optional)
 * for auto-detection and build phases independently. This is useful when a dependency
 * is mandatory in one phase but optional in another.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Config required for auto-detection, optional for build
 * DependencySpec.of(IConfigBuilder.class)
 *     .requireForAutoDetect()
 *     .useForBuild()
 *
 * // InjectionContext optional for auto-detection, required for build
 * DependencySpec.of(IInjectionContextBuilder.class)
 *     .useForAutoDetect()
 *     .requireForBuild()
 *
 * // Required in both phases (equivalent to DependencySpec.require(...))
 * DependencySpec.of(ILoggerBuilder.class)
 *     .requireForAutoDetect()
 *     .requireForBuild()
 *
 * // Optional in both phases (equivalent to DependencySpec.use(...))
 * DependencySpec.of(ICacheBuilder.class)
 *     .useForAutoDetect()
 *     .useForBuild()
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see DependencySpec
 * @see DependencyPhase
 * @see DependencyRequirement
 */
public class DependencySpecBuilder {

    private final Class<? extends IObservableBuilder<?, ?>> dependencyBuilderClass;
    private boolean autoDetectConfigured = false;
    private boolean buildConfigured = false;
    private boolean requiredForAutoDetect = false;
    private boolean requiredForBuild = false;

    /**
     * Creates a new builder for the specified dependency class.
     *
     * @param dependencyClass the class of the dependency builder
     * @throws NullPointerException if dependencyClass is null
     */
    public DependencySpecBuilder(Class<? extends IObservableBuilder<?, ?>> dependencyBuilderClass) {
        this.dependencyBuilderClass = Objects.requireNonNull(dependencyBuilderClass, "Dependency builder class cannot be null");
    }

    /**
     * Marks this dependency as required during auto-detection phase.
     * The dependency will be needed during auto-detection.
     *
     * @return this builder for method chaining
     */
    public DependencySpecBuilder requireForAutoDetect() {
        this.autoDetectConfigured = true;
        this.requiredForAutoDetect = true;
        return this;
    }

    /**
     * Marks this dependency as optional during auto-detection phase.
     * The dependency will be used if available during auto-detection.
     *
     * @return this builder for method chaining
     */
    public DependencySpecBuilder useForAutoDetect() {
        this.autoDetectConfigured = true;
        this.requiredForAutoDetect = false;
        return this;
    }

    /**
     * Marks this dependency as required during build phase.
     * The dependency will be needed during build.
     *
     * @return this builder for method chaining
     */
    public DependencySpecBuilder requireForBuild() {
        this.buildConfigured = true;
        this.requiredForBuild = true;
        return this;
    }

    /**
     * Marks this dependency as optional during build phase.
     * The dependency will be used if available during build.
     *
     * @return this builder for method chaining
     */
    public DependencySpecBuilder useForBuild() {
        this.buildConfigured = true;
        this.requiredForBuild = false;
        return this;
    }

    /**
     * Builds the DependencySpec based on configured requirements.
     *
     * @return the built DependencySpec
     * @throws IllegalStateException if no phases were configured
     */
    public DependencySpec build() {
        if (!autoDetectConfigured && !buildConfigured) {
            throw new IllegalStateException(
                "At least one phase must be configured. " +
                "Use requireForAutoDetect(), useForAutoDetect(), requireForBuild(), or useForBuild()."
            );
        }

        // Determine the phase
        DependencyPhase phase;
        if (autoDetectConfigured && buildConfigured) {
            phase = DependencyPhase.BOTH;
        } else if (autoDetectConfigured) {
            phase = DependencyPhase.AUTO_DETECT;
        } else {
            phase = DependencyPhase.BUILD;
        }

        // Determine the requirement
        DependencyRequirement requirement;
        if (phase == DependencyPhase.BOTH) {
            // Both phases configured - determine if requirements differ
            if (requiredForAutoDetect && requiredForBuild) {
                requirement = DependencyRequirement.REQUIRED;
            } else if (requiredForAutoDetect && !requiredForBuild) {
                requirement = DependencyRequirement.REQUIRED_FOR_AUTO_DETECT;
            } else if (!requiredForAutoDetect && requiredForBuild) {
                requirement = DependencyRequirement.REQUIRED_FOR_BUILD;
            } else {
                requirement = DependencyRequirement.OPTIONAL;
            }
        } else if (phase == DependencyPhase.AUTO_DETECT) {
            requirement = requiredForAutoDetect ?
                DependencyRequirement.REQUIRED : DependencyRequirement.OPTIONAL;
        } else { // BUILD
            requirement = requiredForBuild ?
                DependencyRequirement.REQUIRED : DependencyRequirement.OPTIONAL;
        }

        return new DependencySpec(dependencyBuilderClass, phase, requirement);
    }

    /**
     * Convenience method that builds and returns the DependencySpec.
     * This allows using the builder in a single expression.
     *
     * <p>
     * This method is automatically called when the builder is used in contexts
     * that expect a DependencySpec, but can also be called explicitly.
     * </p>
     *
     * @return the built DependencySpec
     * @see #build()
     */
    public DependencySpec get() {
        return build();
    }
}
