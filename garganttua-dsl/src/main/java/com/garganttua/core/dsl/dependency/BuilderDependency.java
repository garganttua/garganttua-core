package com.garganttua.core.dsl.dependency;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Implementation of builder dependency tracking and lifecycle management.
 *
 * <p>
 * BuilderDependency tracks the state of a dependency on another builder,
 * managing the transition from unresolved to resolved state when the
 * dependency is provided. It implements the observer pattern to receive
 * notifications when the dependency builder produces its built object.
 * </p>
 *
 * <h2>State Management</h2>
 * <p>
 * The dependency state follows a similar pattern to {@code ContextReadinessBuilder}:
 * </p>
 * <ul>
 *   <li><b>isEmpty()</b>: Returns true when both builder and builtObject are null (no dependency provided yet)</li>
 *   <li><b>isReady()</b>: Returns true when builder is not null AND builtObject is not null (dependency fully resolved)</li>
 * </ul>
 *
 * <h2>Dependency Validation Rules</h2>
 * <p>
 * The framework enforces strict validation rules to ensure dependencies are properly initialized:
 * </p>
 * <table border="1">
 *   <caption>Dependency Validation Behavior</caption>
 *   <tr>
 *     <th>Dependency Type</th>
 *     <th>provide() called?</th>
 *     <th>Builder built?</th>
 *     <th>Result</th>
 *     <th>Validation Phase</th>
 *   </tr>
 *   <tr>
 *     <td><b>USE</b> (optional)</td>
 *     <td>No</td>
 *     <td>N/A</td>
 *     <td>OK - Optional dependency not provided</td>
 *     <td>N/A</td>
 *   </tr>
 *   <tr>
 *     <td><b>USE</b> (optional)</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *     <td>OK - Optional dependency ready</td>
 *     <td>Both phases</td>
 *   </tr>
 *   <tr>
 *     <td><b>USE</b> (optional)</td>
 *     <td>Yes</td>
 *     <td>No</td>
 *     <td>DslException - Provided dependency must be built</td>
 *     <td>Both phases</td>
 *   </tr>
 *   <tr>
 *     <td><b>REQUIRE</b> (required)</td>
 *     <td>No</td>
 *     <td>N/A</td>
 *     <td>DslException - Required dependency missing</td>
 *     <td>Both phases</td>
 *   </tr>
 *   <tr>
 *     <td><b>REQUIRE</b> (required)</td>
 *     <td>Yes</td>
 *     <td>Yes</td>
 *     <td>OK - Required dependency ready</td>
 *     <td>Both phases</td>
 *   </tr>
 *   <tr>
 *     <td><b>REQUIRE</b> (required)</td>
 *     <td>Yes</td>
 *     <td>No</td>
 *     <td>DslException - Required dependency not built</td>
 *     <td>Both phases</td>
 *   </tr>
 * </table>
 *
 * <p>
 * <b>Important:</b> For the AUTO_DETECT phase, these validation rules only apply when the builder
 * has {@code autoDetect(true)} enabled. Builders that don't use auto-detection skip AUTO_DETECT
 * phase validation.
 * </p>
 *
 * <h3>Validation Methods</h3>
 * <ul>
 *   <li>{@link #validateUseDependency()} - Validates optional (USE) dependencies</li>
 *   <li>{@link #validateRequiredDependency(String)} - Validates required (REQUIRE) dependencies</li>
 * </ul>
 *
 * <p><b>Size note:</b> this class exceeds the 500-line advisory threshold but is a
 * cohesive {@link IBuilderDependency} implementation — its length is mandatory
 * single-line query accessors plus thorough javadoc and per-accessor trace logging,
 * not extractable multi-responsibility logic (validation needs deep internal state;
 * a firing-memory wrapper would only add delegation). Kept whole by design per the
 * alpha02 rework rules.</p>
 *
 * @param <Builder> the type of the observable builder being depended upon
 * @param <Built> the type of object built by the dependency
 * @since 2.0.0-ALPHA01
 */
public class BuilderDependency<Builder extends IObservableBuilder<Builder, Built>, Built>
        implements IBuilderDependency<Builder, Built> {
    private static final Logger log = Logger.getLogger(BuilderDependency.class);

    private static final String LOG_PRESENT = "present";
    private static final String LOG_ABSENT = "absent";

    private final IClass<Builder> dependencyClass;
    private final DependencySpec spec;
    private Builder builder;
    private Built builtObject;
    private final Set<String> packages = new HashSet<>();
    /**
     * Tracks which firing events have already happened for this dependency
     * over the Bootstrap lifetime — used to enforce the idempotency contract.
     * Bootstrap consults this set before invoking a hook so the same
     * (consumer, dep, event) tuple never fires twice, even across
     * {@code rebuild()}.
     */
    private final EnumSet<FiringEvent> fired = EnumSet.noneOf(FiringEvent.class);

    /**
     * Identifies an individual hook invocation for idempotency tracking.
     * A single {@link DependencySpec} may yield up to four events depending
     * on its stage (CONFIGURATION, AUTO_DETECT, PRE_BUILD, POST_BUILD); a
     * stage-BUILD spec always fires PRE_BUILD then POST_BUILD which is why
     * the BUILD stage maps to two distinct events.
     */
    public enum FiringEvent {
        CONFIGURATION,
        AUTO_DETECT,
        PRE_BUILD,
        POST_BUILD
    }

    /**
     * Creates a new phase-aware builder dependency.
     *
     * @param dependencyClass the class of the builder being depended upon
     * @param spec the dependency specification including phase information
     */
    @SuppressWarnings("unchecked")
    public BuilderDependency(IClass<? extends IObservableBuilder<?, ?>> dependencyClass, DependencySpec spec) {
        log.trace("Creating phase-aware BuilderDependency for class: {} with phase: {}",
            dependencyClass, spec.phase());
        this.dependencyClass = (IClass<Builder>) Objects.requireNonNull(dependencyClass,
            "Dependency class cannot be null");
        this.spec = Objects.requireNonNull(spec, "Dependency spec cannot be null");
        log.debug("BuilderDependency created for: {}, phase: {}, isReady: {}, isEmpty: {}",
            this.dependencyClass.getName(), spec.phase(), isReady(), isEmpty());
    }

    /**
     * Stores the builder reference for this dependency.
     *
     * <p>This method ONLY captures the reference — it does <strong>not</strong>
     * attempt to build the upstream builder. Build orchestration is the
     * caller's job (Bootstrap walks builders in dependency order). The built
     * value is fetched lazily later via {@link #tryResolve()}, called only
     * by {@code processXxxDependencies} at the point where each hook fires
     * — i.e. inside the dependent's own {@code build()}, by which time the
     * upstream is guaranteed to have been built (and lifecycle-started by
     * Bootstrap) on the canonical path.
     *
     * @param observableBuilder the observable builder providing the dependency
     */
    @SuppressWarnings("unchecked")
    void handle(IObservableBuilder<?, ?> observableBuilder) {
        log.trace("Handling observableBuilder provision: {}", observableBuilder);
        if (!dependencyClass.isAssignableFrom(observableBuilder.getClass())) {
            log.warn("Dependency type mismatch: expected {}, got {}",
                dependencyClass.getName(), observableBuilder.getClass().getName());
            return;
        }

        this.builder = (Builder) observableBuilder;
        log.debug("Dependency builder stored (build deferred to orchestrator): {}",
                dependencyClass.getName());
    }

    /**
     * Records the built object produced by the dependency, marking it resolvable.
     *
     * @param observable the built object
     * @throws NullPointerException if {@code observable} is {@code null}
     */
    @Override
    public void handle(Built observable) {
        log.trace("Handling built object notification: {}", observable);
        this.builtObject = Objects.requireNonNull(observable, "Built object cannot be null");
        log.debug("Dependency marked with built object: {}, isReady: {}", observable, isReady());
    }

    /**
     * Attempts to resolve the built object from the builder if not already resolved.
     * This supports deferred resolution where the builder may be provided during
     * dependency resolution (Phase 1) but only built later (Phase 2). Build failures
     * are swallowed (logged at trace) since the upstream may simply not be ready yet.
     */
    private void tryResolve() {
        if (builder != null && builtObject == null) {
            try {
                this.builtObject = this.builder.build();
                log.debug("Dependency lazily resolved: {} -> {}", dependencyClass.getName(), builtObject);
            } catch (DslException e) {
                log.trace("Dependency {} not yet buildable: {}", dependencyClass.getName(), e.getMessage());
            }
        }
    }

    @Override
    public boolean isReady() {
        tryResolve();
        boolean ready = builder != null && builtObject != null;
        log.trace("Checking if dependency is ready: {} (builder: {}, builtObject: {})",
            ready,
            builder != null ? LOG_PRESENT : LOG_ABSENT,
            builtObject != null ? LOG_PRESENT : LOG_ABSENT);
        return ready;
    }

    /**
     * Checks if the dependency is empty (not yet provided).
     *
     * <p>
     * The dependency is considered empty when neither the builder nor the built object
     * have been provided. This mirrors the pattern from ContextReadinessBuilder where
     * an empty state means no components have been set.
     * </p>
     *
     * @return true if both builder and builtObject are null, false otherwise
     */
    @Override
    public boolean isEmpty() {
        boolean empty = builder == null && builtObject == null;
        log.trace("Checking if dependency is empty: {} (builder: {}, builtObject: {})",
            empty,
            builder != null ? LOG_PRESENT : LOG_ABSENT,
            builtObject != null ? LOG_PRESENT : LOG_ABSENT);
        return empty;
    }

    @Override
    public IClass<Builder> getDependency() {
        log.trace("Getting dependency class: {}", dependencyClass);
        return dependencyClass;
    }

    @Override
    public Built get() {
        tryResolve();
        log.trace("Getting built object: {}", builtObject);
        if (!isReady()) {
            log.warn("Attempting to get built object from non-ready dependency - builder: {}, builtObject: {}",
                builder != null ? LOG_PRESENT : LOG_ABSENT,
                builtObject != null ? LOG_PRESENT : LOG_ABSENT);
            throw new IllegalStateException("Dependency is not ready: " + dependencyClass.getName());
        }
        return builtObject;
    }

    @Override
    public Builder builder() {
        log.trace("Getting builder: {}", builder);
        if (builder == null) {
            log.warn("Attempting to get null builder");
            throw new IllegalStateException("Builder not yet provided for: " + dependencyClass.getName());
        }
        return builder;
    }

    @Override
    public void ifReady(Consumer<Built> consumer) {
        log.trace("Executing ifReady with consumer");
        if (isReady()) {
            log.debug("Dependency is ready, executing consumer");
            consumer.accept(builtObject);
        } else {
            log.debug("Dependency not ready, skipping consumer");
        }
    }

    @Override
    public void ifReadyOrElse(Consumer<Built> consumer, Runnable fallbackAction) {
        log.trace("Executing ifReadyOrElse");
        if (isReady()) {
            log.debug("Dependency is ready, executing consumer");
            consumer.accept(builtObject);
        } else {
            log.debug("Dependency not ready, executing fallback action");
            fallbackAction.run();
        }
    }

    @Override
    public void ifReadyOrElseThrow(Consumer<Built> consumer) {
        log.trace("Executing ifReadyOrElseThrow");
        if (!isReady()) {
            log.error("Dependency not ready, throwing exception - builder: {}, builtObject: {}",
                builder != null ? LOG_PRESENT : LOG_ABSENT,
                builtObject != null ? LOG_PRESENT : LOG_ABSENT);
            throw new IllegalStateException("Dependency is not ready: " + dependencyClass.getName());
        }
        
        log.debug("Dependency is ready, executing consumer");
        consumer.accept(builtObject);
    }

    @Override
    public <X extends Throwable> void ifReadyOrElseThrow(
            Consumer<Built> consumer,
            Supplier<? extends X> exceptionSupplier) throws X {
        log.trace("Executing ifReadyOrElseThrow with custom exception");
        if (!isReady()) {
            log.error("Dependency not ready, throwing custom exception - builder: {}, builtObject: {}",
                builder != null ? LOG_PRESENT : LOG_ABSENT,
                builtObject != null ? LOG_PRESENT : LOG_ABSENT);
            throw exceptionSupplier.get();
        }
        
        log.debug("Dependency is ready, executing consumer");
        consumer.accept(builtObject);
    }

    @Override
    public void synchronizePackagesFromContext(Consumer<Set<String>> packageConsumer) {
        log.trace("Synchronizing packages from context");
        // Read packages from the stored builder if it is packageable,
        // since the local packages set is not populated via provide().
        if (builder instanceof IPackageableBuilder<?, ?> packageable) {
            String[] builderPackages = packageable.getPackages();
            if (builderPackages != null && builderPackages.length > 0) {
                Set<String> pkgSet = Set.of(builderPackages);
                packageConsumer.accept(pkgSet);
                log.debug("Packages synchronized from builder: {}", pkgSet.size());
                return;
            }
        }
        packageConsumer.accept(packages);
        log.debug("Packages synchronized (local): {}", packages.size());
    }

    /**
     * Gets the dependency specification containing phase information.
     *
     * @return the dependency specification
     */
    public DependencySpec getSpec() {
        return spec;
    }

    /**
     * Checks if this dependency is needed during auto-detection phase.
     *
     * @return true if needed during auto-detection
     */
    public boolean isNeededForAutoDetect() {
        return spec.isNeededForAutoDetect();
    }

    /**
     * Checks if this dependency is needed during build phase.
     *
     * @return true if needed during build
     */
    public boolean isNeededForBuild() {
        return spec.isNeededForBuild();
    }

    /**
     * Checks if this dependency is required during auto-detection phase.
     *
     * @return true if required during auto-detection
     */
    public boolean isRequiredForAutoDetect() {
        return spec.isRequiredForAutoDetect();
    }

    /**
     * Checks if this dependency is required during build phase.
     *
     * @return true if required during build
     */
    public boolean isRequiredForBuild() {
        return spec.isRequiredForBuild();
    }

    /**
     * Checks if this dependency is optional during auto-detection phase.
     *
     * @return true if optional during auto-detection
     */
    public boolean isOptionalForAutoDetect() {
        return spec.isOptionalForAutoDetect();
    }

    /**
     * Checks if this dependency is optional during build phase.
     *
     * @return true if optional during build
     */
    public boolean isOptionalForBuild() {
        return spec.isOptionalForBuild();
    }

    /**
     * Asserts that this dependency has been provided in some form.
     *
     * @throws IllegalStateException if the dependency is empty (neither builder nor built object set)
     */
    @Override
    public void requireNotEmpty() {
        if(this.isEmpty())
            throw new IllegalStateException("Dependency is empty");
    }

    // ----------------------------------------------------------------------
    // Stage / Kind accessors (new vocabulary, mirror DependencySpec)
    // ----------------------------------------------------------------------

    /** @return the dependency stage (CONFIGURATION, AUTO_DETECT or BUILD). */
    public DependencyStage stage() {
        return spec.stage();
    }

    /** @return the dependency kind (BUILDER or BUILT). */
    public DependencyKind kind() {
        return spec.kind();
    }

    /** @return the underlying dependency specification. */
    public DependencySpec spec() {
        return spec;
    }

    /** @return {@code true} if this dependency is required (not optional). */
    public boolean isRequired() {
        return spec.isRequired();
    }

    /** @return {@code true} if this dependency is optional. */
    public boolean isOptional() {
        return spec.isOptional();
    }

    /** @return {@code true} when {@link #handle(IObservableBuilder)} has been called. */
    public boolean hasBuilder() {
        return this.builder != null;
    }

    /** @return {@code true} when {@link #handle(Object)} has been called. */
    public boolean hasBuilt() {
        return this.builtObject != null;
    }

    /** @return {@code true} if this dependency belongs to the CONFIGURATION stage. */
    public boolean isConfigurationStage() {
        return spec.stage() == DependencyStage.CONFIGURATION;
    }

    /** @return {@code true} if this dependency belongs to the AUTO_DETECT stage. */
    public boolean isAutoDetectStage() {
        return spec.stage() == DependencyStage.AUTO_DETECT;
    }

    /** @return {@code true} if this dependency belongs to the BUILD stage. */
    public boolean isBuildStage() {
        return spec.stage() == DependencyStage.BUILD;
    }

    /** @return {@code true} if this dependency is satisfied with the upstream builder reference. */
    public boolean isBuilderKind() {
        return spec.kind() == DependencyKind.BUILDER;
    }

    /** @return {@code true} if this dependency is satisfied with the upstream built object. */
    public boolean isBuiltKind() {
        return spec.kind() == DependencyKind.BUILT;
    }

    // ----------------------------------------------------------------------
    // Idempotency tracking — Bootstrap (or any orchestrator) calls
    // tryMarkFired before invoking a hook. The Set tracks fired events for
    // the lifetime of this BuilderDependency instance.
    // ----------------------------------------------------------------------

    /**
     * Mark a firing event as having happened. Returns {@code true} if the
     * event had not been recorded before (caller should fire the hook),
     * {@code false} if it had already fired (caller must skip).
     */
    public boolean tryMarkFired(FiringEvent event) {
        Objects.requireNonNull(event, "event");
        return this.fired.add(event);
    }

    /**
     * @return {@code true} if the given firing event has already happened.
     */
    public boolean hasFired(FiringEvent event) {
        return this.fired.contains(event);
    }

    /**
     * Reset the firing-event memory. The orchestrator may call this
     * between a fresh build and a {@code rebuild()} to allow per-build
     * hooks (PRE_BUILD / POST_BUILD / AUTO_DETECT) to fire again — note
     * that CONFIGURATION is intentionally <strong>not</strong> reset by
     * default, since it is meant to fire at most once per Bootstrap
     * lifetime regardless of rebuilds.
     */
    public void resetFiringMemory(boolean keepConfiguration) {
        if (keepConfiguration) {
            boolean configured = this.fired.contains(FiringEvent.CONFIGURATION);
            this.fired.clear();
            if (configured) {
                this.fired.add(FiringEvent.CONFIGURATION);
            }
        } else {
            this.fired.clear();
        }
    }

    /**
     * Validates that if a dependency was provided via provide(), it must be built.
     * This applies to "use" (optional) dependencies.
     *
     * @throws DslException if provide() was called but the builder is not built
     */
    public void validateUseDependency() throws DslException {
        tryResolve();
        // If builder was provided but not built, throw exception
        if (builder != null && builtObject == null) {
            String errorMsg = String.format(
                "Dependency %s was provided via provide() but has not been built. " +
                "Optional dependencies (use) must be built before they can be used.",
                dependencyClass.getName());
            log.error(errorMsg);
            throw new DslException(errorMsg);
        }
    }

    /**
     * Validates that a required dependency has been provided and built.
     * This applies to "require" (required) dependencies.
     *
     * @param phase the phase being validated (for error messaging)
     * @throws DslException if the required dependency is not provided or not built
     */
    public void validateRequiredDependency(String phase) throws DslException {
        tryResolve();
        // If neither builder nor built object provided, throw exception
        if (isEmpty()) {
            String errorMsg = String.format(
                "Required dependency %s for phase %s was not provided. " +
                "Required dependencies must be provided via provide() and built.",
                dependencyClass.getName(), phase);
            log.debug(errorMsg);
            throw new DslException(errorMsg)
                    .withStageFailure(stageFailure(phase, "not provided", null));
        }

        // If builder provided but not built, throw exception
        if (builder != null && builtObject == null) {
            String errorMsg = String.format(
                "Required dependency %s for phase %s was provided but not built. " +
                "The dependency builder must be built before use.",
                dependencyClass.getName(), phase);
            log.debug(errorMsg);
            throw new DslException(errorMsg)
                    .withStageFailure(stageFailure(phase, "provided but not built", null));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private StageFailureContext stageFailure(String phaseLabel, String reason, Throwable cause) {
        DependencyStage stage;
        try {
            stage = DependencyStage.valueOf(phaseLabel);
        } catch (IllegalArgumentException notAStage) {
            stage = this.spec.stage();
        }
        return new StageFailureContext(
                null,
                (IClass) this.dependencyClass,
                stage,
                this.spec.kind(),
                reason,
                cause);
    }
}
