package com.garganttua.core.dsl.dependency;

import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Specification of a dependency, declared by a builder that consumes another
 * builder.
 *
 * <p>A {@code DependencySpec} pins down three orthogonal answers:
 * <ul>
 *   <li><strong>What</strong> — {@link #dependencyBuilderClass()} : the class
 *       of the upstream builder.</li>
 *   <li><strong>When</strong> — {@link #stage()} : the lifecycle stage of
 *       the consumer at which the hook fires
 *       ({@link DependencyStage#CONFIGURATION},
 *       {@link DependencyStage#AUTO_DETECT} or
 *       {@link DependencyStage#BUILD}).</li>
 *   <li><strong>How</strong> — {@link #kind()} : the form in which the
 *       dependency is delivered ({@link DependencyKind#BUILDER builder}
 *       reference or {@link DependencyKind#BUILT built} object).</li>
 * </ul>
 * Plus {@link #requirement()} which controls strict vs optional resolution.
 *
 * <h2>Stage / Kind compatibility</h2>
 * <ul>
 *   <li>{@code CONFIGURATION + BUILDER} : valid. The dependency hasn't been
 *       built yet at this stage so {@link DependencyKind#BUILT} is rejected.</li>
 *   <li>{@code AUTO_DETECT + BUILDER}   : valid (rare).</li>
 *   <li>{@code AUTO_DETECT + BUILT}     : valid — upstream must be built
 *       first; topological ordering guarantees it.</li>
 *   <li>{@code BUILD + BUILDER}         : valid.</li>
 *   <li>{@code BUILD + BUILT}           : valid — the standard combo, also
 *       the default for the legacy {@code use(class)} / {@code require(class)}
 *       factories.</li>
 * </ul>
 *
 * <h2>Idempotency contract</h2>
 * <p>Hooks fired at any stage <strong>must</strong> be idempotent. Bootstrap
 * fires each {@code (consumer, dep, stage, kind)} tuple at most once per
 * Bootstrap lifetime, including across {@code rebuild()}, but authors must
 * still design hook side-effects to tolerate accidental double-invocation.
 *
 * @param dependencyBuilderClass the class of the dependency builder
 * @param stage                  when the hook fires
 * @param kind                   what the hook receives
 * @param requirement            strict (required) vs lenient (optional)
 *                               resolution
 * @since 2.0.0-ALPHA02
 * @see DependencyStage
 * @see DependencyKind
 * @see DependencyRequirement
 */
public record DependencySpec(
        IClass<? extends IObservableBuilder<?, ?>> dependencyBuilderClass,
        DependencyStage stage,
        DependencyKind kind,
        DependencyRequirement requirement) {

    /**
     * Canonical constructor. Validates non-null inputs and rejects the
     * impossible {@code CONFIGURATION + BUILT} pairing.
     */
    public DependencySpec {
        Objects.requireNonNull(dependencyBuilderClass, "Dependency builder class cannot be null");
        Objects.requireNonNull(stage, "Dependency stage cannot be null");
        Objects.requireNonNull(kind, "Dependency kind cannot be null");
        Objects.requireNonNull(requirement, "Dependency requirement cannot be null");

        if (stage == DependencyStage.CONFIGURATION && kind == DependencyKind.BUILT) {
            throw new IllegalArgumentException(
                    "Invalid DependencySpec: CONFIGURATION stage cannot have kind BUILT — "
                            + "no built object exists at configuration time. "
                            + "Use kind BUILDER instead.");
        }

        // Phase-specific requirement values only made sense with the legacy
        // BOTH phase; with the per-stage model they no longer apply.
        if (requirement == DependencyRequirement.REQUIRED_FOR_AUTO_DETECT
                || requirement == DependencyRequirement.REQUIRED_FOR_BUILD) {
            throw new IllegalArgumentException(
                    "Phase-specific requirements REQUIRED_FOR_AUTO_DETECT / REQUIRED_FOR_BUILD "
                            + "are obsolete with the per-stage DependencySpec. "
                            + "Declare two DependencySpec entries — one per stage — instead.");
        }
    }

    // ---------------------------------------------------------------------
    // Factory helpers — single-stage construction
    // ---------------------------------------------------------------------

    /**
     * {@code CONFIGURATION + BUILDER + OPTIONAL}. The consumer receives the
     * upstream builder during the global configuration phase and may apply
     * configuration on it before any build runs.
     */
    public static DependencySpec configure(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.CONFIGURATION, DependencyKind.BUILDER,
                DependencyRequirement.OPTIONAL);
    }

    /**
     * {@code CONFIGURATION + BUILDER + REQUIRED}.
     */
    public static DependencySpec requireConfigure(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.CONFIGURATION, DependencyKind.BUILDER,
                DependencyRequirement.REQUIRED);
    }

    /**
     * {@code AUTO_DETECT + BUILT + OPTIONAL}.
     */
    public static DependencySpec autoDetect(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.AUTO_DETECT, DependencyKind.BUILT,
                DependencyRequirement.OPTIONAL);
    }

    /**
     * {@code AUTO_DETECT + BUILT + REQUIRED}.
     */
    public static DependencySpec requireAutoDetect(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.AUTO_DETECT, DependencyKind.BUILT,
                DependencyRequirement.REQUIRED);
    }

    /**
     * {@code AUTO_DETECT + BUILDER + OPTIONAL}.
     */
    public static DependencySpec autoDetectBuilder(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.AUTO_DETECT, DependencyKind.BUILDER,
                DependencyRequirement.OPTIONAL);
    }

    /**
     * {@code BUILD + BUILT + OPTIONAL}. The legacy default kept for
     * backward compatibility — the most common dependency shape.
     */
    public static DependencySpec use(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.BUILD, DependencyKind.BUILT,
                DependencyRequirement.OPTIONAL);
    }

    /**
     * {@code BUILD + BUILT + REQUIRED}. Legacy default for required deps.
     */
    public static DependencySpec require(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.BUILD, DependencyKind.BUILT,
                DependencyRequirement.REQUIRED);
    }

    /**
     * {@code BUILD + BUILDER + OPTIONAL}.
     */
    public static DependencySpec useBuilder(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.BUILD, DependencyKind.BUILDER,
                DependencyRequirement.OPTIONAL);
    }

    /**
     * {@code BUILD + BUILDER + REQUIRED}.
     */
    public static DependencySpec requireBuilder(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpec(dependencyClass,
                DependencyStage.BUILD, DependencyKind.BUILDER,
                DependencyRequirement.REQUIRED);
    }

    // ---------------------------------------------------------------------
    // Multi-stage helper — convenience for the common "config + build" combo
    // ---------------------------------------------------------------------

    /**
     * Build a pair of {@link DependencySpec} entries — one for
     * {@link DependencyStage#CONFIGURATION CONFIGURATION} (always
     * {@link DependencyKind#BUILDER}) and one for the requested
     * post-config stage (typically {@link DependencyStage#BUILD}).
     *
     * <p>Common pattern: the consumer needs to mutate the upstream's
     * builder during configuration, then receive the built result later.
     *
     * <pre>{@code
     * super(DependencySpec.configureAndStage(
     *         IClass.getClass(IInjectionContextBuilder.class),
     *         DependencyStage.BUILD, DependencyKind.BUILT,
     *         DependencyRequirement.OPTIONAL));
     * }</pre>
     */
    public static Set<DependencySpec> configureAndStage(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass,
            DependencyStage postStage,
            DependencyKind postKind,
            DependencyRequirement requirement) {
        return Set.of(
                new DependencySpec(dependencyClass,
                        DependencyStage.CONFIGURATION, DependencyKind.BUILDER,
                        requirement),
                new DependencySpec(dependencyClass, postStage, postKind, requirement));
    }

    /**
     * @return a {@link DependencySpecBuilder} for fluent fine-grained
     *         configuration of a single {@link DependencySpec}.
     */
    public static DependencySpecBuilder of(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass) {
        return new DependencySpecBuilder(dependencyClass);
    }

    /**
     * Discover every {@link DependsOn} annotation on the given builder class
     * (walking up the class hierarchy) and convert each to a
     * {@link DependencySpec}. Returns an empty set when no annotations are
     * present.
     *
     * <p>Walks superclasses so a base class can declare common deps; doesn't
     * walk implemented interfaces (the JVM doesn't make
     * {@code @Repeatable}-style annotations easy to read across interfaces
     * anyway).
     *
     * <p>This is the main entry-point used by the abstract dependent-builder
     * bases to merge declarative {@code @DependsOn} with the imperative
     * {@code Set<DependencySpec>} passed to their super-constructor.
     */
    @SuppressWarnings("unchecked")
    public static java.util.Set<DependencySpec> fromAnnotations(Class<?> builderClass) {
        if (builderClass == null) {
            return java.util.Set.of();
        }
        java.util.Set<DependencySpec> specs = new java.util.LinkedHashSet<>();
        for (Class<?> c = builderClass; c != null && c != Object.class; c = c.getSuperclass()) {
            for (DependsOn ann : c.getAnnotationsByType(DependsOn.class)) {
                specs.add(new DependencySpec(
                        IClass.getClass((Class<? extends IObservableBuilder<?, ?>>) ann.target()),
                        ann.stage(),
                        ann.kind(),
                        ann.requirement()));
            }
        }
        return specs;
    }

    // ---------------------------------------------------------------------
    // Convenience predicates
    // ---------------------------------------------------------------------

    /** @return {@code true} if the stage is {@link DependencyStage#CONFIGURATION}. */
    public boolean isConfiguration() {
        return this.stage == DependencyStage.CONFIGURATION;
    }

    /** @return {@code true} if the stage is {@link DependencyStage#AUTO_DETECT}. */
    public boolean isAutoDetect() {
        return this.stage == DependencyStage.AUTO_DETECT;
    }

    /** @return {@code true} if the stage is {@link DependencyStage#BUILD}. */
    public boolean isBuild() {
        return this.stage == DependencyStage.BUILD;
    }

    /** @return {@code true} if the kind is {@link DependencyKind#BUILDER}. */
    public boolean isBuilderKind() {
        return this.kind == DependencyKind.BUILDER;
    }

    /** @return {@code true} if the kind is {@link DependencyKind#BUILT}. */
    public boolean isBuiltKind() {
        return this.kind == DependencyKind.BUILT;
    }

    /** @return {@code true} if the requirement is {@link DependencyRequirement#REQUIRED}. */
    public boolean isRequired() {
        return this.requirement == DependencyRequirement.REQUIRED;
    }

    /** @return {@code true} if the requirement is {@link DependencyRequirement#OPTIONAL}. */
    public boolean isOptional() {
        return this.requirement == DependencyRequirement.OPTIONAL;
    }

    // ---------------------------------------------------------------------
    // Legacy compatibility surface — back-compat for callers still using
    // the DependencyPhase vocabulary. Will be removed once all internal
    // builders migrate to single-stage declarations.
    // ---------------------------------------------------------------------

    /**
     * @deprecated use {@link #stage()} + {@link #kind()} instead. This
     *             accessor maps the new stage back to the legacy phase
     *             enum for any code still reading it. CONFIGURATION maps
     *             to the most appropriate legacy value (AUTO_DETECT for
     *             builder-kind, BUILD otherwise).
     */
    @Deprecated(forRemoval = true)
    public DependencyPhase phase() {
        return switch (this.stage) {
            case CONFIGURATION -> DependencyPhase.AUTO_DETECT;
            case AUTO_DETECT -> DependencyPhase.AUTO_DETECT;
            case BUILD -> DependencyPhase.BUILD;
        };
    }

    /** @deprecated use {@link #isConfiguration()} / {@link #isAutoDetect()}. */
    @Deprecated(forRemoval = true)
    public boolean isNeededForAutoDetect() {
        return this.stage == DependencyStage.CONFIGURATION
                || this.stage == DependencyStage.AUTO_DETECT;
    }

    /** @deprecated use {@link #isBuild()}. */
    @Deprecated(forRemoval = true)
    public boolean isNeededForBuild() {
        return this.stage == DependencyStage.BUILD;
    }

    /** @deprecated use {@link #isRequired()}. */
    @Deprecated(forRemoval = true)
    public boolean isRequiredForAutoDetect() {
        return this.isRequired() && this.isNeededForAutoDetect();
    }

    /** @deprecated use {@link #isRequired()}. */
    @Deprecated(forRemoval = true)
    public boolean isRequiredForBuild() {
        return this.isRequired() && this.isNeededForBuild();
    }

    /** @deprecated use {@link #isOptional()}. */
    @Deprecated(forRemoval = true)
    public boolean isOptionalForAutoDetect() {
        return this.isOptional() && this.isNeededForAutoDetect();
    }

    /** @deprecated use {@link #isOptional()}. */
    @Deprecated(forRemoval = true)
    public boolean isOptionalForBuild() {
        return this.isOptional() && this.isNeededForBuild();
    }

    /**
     * @deprecated use the new single-stage factories ({@link #autoDetect},
     *             {@link #use}, {@link #configure}…) or
     *             {@link #configureAndStage} for the multi-stage case.
     */
    @Deprecated(forRemoval = true)
    public static DependencySpec use(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass,
            DependencyPhase phase) {
        return new DependencySpec(dependencyClass, fromLegacy(phase), DependencyKind.BUILT,
                DependencyRequirement.OPTIONAL);
    }

    /** @deprecated see {@link #use(IClass, DependencyPhase)}. */
    @Deprecated(forRemoval = true)
    public static DependencySpec require(
            IClass<? extends IObservableBuilder<?, ?>> dependencyClass,
            DependencyPhase phase) {
        return new DependencySpec(dependencyClass, fromLegacy(phase), DependencyKind.BUILT,
                DependencyRequirement.REQUIRED);
    }

    /**
     * Map a legacy {@link DependencyPhase} to a {@link DependencyStage}.
     * {@code BOTH} is not representable as a single stage — callers
     * relying on it must declare two {@link DependencySpec} entries.
     */
    private static DependencyStage fromLegacy(DependencyPhase phase) {
        return switch (phase) {
            case AUTO_DETECT -> DependencyStage.AUTO_DETECT;
            case BUILD -> DependencyStage.BUILD;
            case BOTH -> throw new IllegalArgumentException(
                    "DependencyPhase.BOTH is no longer supported as a single DependencySpec "
                            + "— declare two entries (one AUTO_DETECT, one BUILD) instead.");
        };
    }
}
