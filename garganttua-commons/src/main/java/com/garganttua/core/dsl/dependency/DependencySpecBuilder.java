package com.garganttua.core.dsl.dependency;

import java.util.Objects;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder for creating a single-stage {@link DependencySpec}.
 *
 * <p>
 * Exactly one phase must be configured per builder: either the auto-detect
 * phase ({@link #requireForAutoDetect()} / {@link #useForAutoDetect()}) or the
 * build phase ({@link #requireForBuild()} / {@link #useForBuild()}). Configuring
 * both on the same chain is rejected by {@link #build()} — declare two separate
 * {@link DependencySpec} entries, or use {@link DependencySpec#configureAndStage}
 * for the configuration-then-build pattern.
 * </p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Required for auto-detection
 * DependencySpec.of(IConfigBuilder.class)
 *     .requireForAutoDetect()
 *     .build();
 *
 * // Optional for build
 * DependencySpec.of(ICacheBuilder.class)
 *     .useForBuild()
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see DependencySpec
 * @see DependencyStage
 * @see DependencyRequirement
 */
@Reflected
public class DependencySpecBuilder {

    private final IClass<? extends IObservableBuilder<?, ?>> dependencyBuilderClass;
    private boolean autoDetectConfigured = false;
    private boolean buildConfigured = false;
    private boolean requiredForAutoDetect = false;
    private boolean requiredForBuild = false;

    /**
     * Creates a new builder for the specified dependency class.
     *
     * @param dependencyBuilderClass the class of the dependency builder
     * @throws NullPointerException if dependencyClass is null
     */
    public DependencySpecBuilder(IClass<? extends IObservableBuilder<?, ?>> dependencyBuilderClass) {
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
     * Builds a single-stage {@link DependencySpec}.
     *
     * <p>Configuring BOTH auto-detect AND build phases on the same fluent
     * chain is no longer supported — declare two separate {@link DependencySpec}
     * entries instead, one per stage. Use {@link DependencySpec#configureAndStage}
     * for the common configuration-then-build pattern.
     *
     * <p>{@link DependencyKind} defaults to {@link DependencyKind#BUILT}
     * (the legacy shape); use the new factory helpers on {@link DependencySpec}
     * for {@link DependencyKind#BUILDER} declarations.
     *
     * @return the built DependencySpec
     * @throws IllegalStateException if no phase, or both phases, were configured
     */
    public DependencySpec build() {
        if (!autoDetectConfigured && !buildConfigured) {
            throw new IllegalStateException(
                "At least one phase must be configured. " +
                "Use requireForAutoDetect(), useForAutoDetect(), requireForBuild(), or useForBuild()."
            );
        }
        if (autoDetectConfigured && buildConfigured) {
            throw new IllegalStateException(
                "DependencySpecBuilder no longer supports the BOTH-phase shorthand. "
                + "Declare two DependencySpec entries (one for AUTO_DETECT, one for BUILD) "
                + "or use DependencySpec.configureAndStage(...) for the typical "
                + "configuration-then-build pattern.");
        }

        DependencyStage stage = autoDetectConfigured
                ? DependencyStage.AUTO_DETECT
                : DependencyStage.BUILD;
        DependencyRequirement requirement = (autoDetectConfigured ? requiredForAutoDetect : requiredForBuild)
                ? DependencyRequirement.REQUIRED
                : DependencyRequirement.OPTIONAL;

        return new DependencySpec(dependencyBuilderClass, stage, DependencyKind.BUILT, requirement);
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
