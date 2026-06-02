package com.garganttua.core.dsl.dependency;

import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Abstract builder combining automatic detection and dependency management.
 *
 * <p>
 * This class extends {@link AbstractAutomaticBuilder} and implements {@link IDependentBuilder}
 * to provide both auto-detection capabilities and the ability to declare and track
 * dependencies on other builders. It manages the full lifecycle including dependency-aware
 * auto-detection.
 * </p>
 *
 * <h2>Dependency Lifecycle</h2>
 * <p>
 * Dependencies are managed through three main phases:
 * </p>
 * <ol>
 *   <li><b>Declaration</b>: Dependencies are declared in the constructor via use/require sets</li>
 *   <li><b>Provision</b>: Concrete dependency instances are provided via {@code provide()}</li>
 *   <li><b>Resolution</b>: During {@code build()}, dependencies are validated and processed</li>
 * </ol>
 *
 * <h2>Build Process with Auto-Detection</h2>
 * <p>
 * The build process follows this sequence:
 * </p>
 * <ol>
 *   <li>Auto-detection: If enabled, {@code doAutoDetection()} is called</li>
 *   <li>Auto-detection with dependencies: {@code doAutoDetectionWithDependency()} for each ready dependency</li>
 *   <li>Pre-build: Process ready dependencies via {@code doPreBuildWithDependency()}</li>
 *   <li>Build: Execute {@code doBuild()} to create the target object</li>
 *   <li>Post-build: Process ready dependencies via {@code doPostBuildWithDependency()}</li>
 * </ol>
 *
 * @param <B> the concrete builder type for method chaining
 * @param <T> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see AbstractAutomaticBuilder
 * @see IDependentBuilder
 */
public abstract class AbstractAutomaticDependentBuilder<B extends IBuilder<T>, T>
        extends AbstractAutomaticBuilder<B, T>
        implements IDependentBuilder<B, T> {
    private static final Logger log = Logger.getLogger(AbstractAutomaticDependentBuilder.class);

    protected final DependentBuilderSupport support;

    /**
     * Constructs a new AbstractAutomaticDependentBuilder with specified dependency sets.
     *
     * @param dependencies the set of dependency specifications
     * @throws NullPointerException if either parameter is null
     */
    protected AbstractAutomaticDependentBuilder(
            Set<DependencySpec> dependencies) {
        super();
        log.trace("Entering AbstractAutomaticDependentBuilder constructor");
        Set<DependencySpec> merged = new java.util.LinkedHashSet<>(dependencies);
        merged.addAll(DependencySpec.fromAnnotations(this.getClass()));
        this.support = new DependentBuilderSupport(merged);
        log.trace("Exiting AbstractAutomaticDependentBuilder constructor");
    }

    /**
     * Supplies a concrete dependency builder instance to satisfy a declared dependency.
     *
     * @param dependency the dependency builder to provide
     * @return this builder for method chaining
     * @throws DslException if the dependency is not declared in {@link #use()} or {@link #require()}
     */
    @SuppressWarnings("unchecked")
    @Override
    public B provide(IObservableBuilder<?, ?> dependency) throws DslException {
        this.support.provide(dependency);
        return (B) this;
    }

    /**
     * Returns the classes of optional (use) dependencies declared by this builder.
     *
     * @return the set of optional dependency classes
     */
    @Override
    public Set<IClass<? extends IObservableBuilder<?, ?>>> use() {
        return this.support.use();
    }

    /**
     * Returns the classes of required dependencies declared by this builder.
     *
     * @return the set of required dependency classes
     */
    @Override
    public Set<IClass<? extends IObservableBuilder<?, ?>>> require() {
        return this.support.require();
    }

    /**
     * Builds the target object, running auto-detection (if enabled) and the
     * pre-build and post-build dependency phases. The result is cached on
     * first success and returned directly on subsequent calls.
     *
     * @return the built object
     * @throws DslException if a dependency phase or {@code doBuild()} fails
     */
    @Override
    public T build() throws DslException {
        log.trace("Entering build method");

        if (this.built != null) {
            log.debug("Returning previously built instance: {}", this.built);
            log.trace("Exiting build method (cached)");
            return this.built;
        }

        try {
            // Phase 1: Auto-detection
            if (this.autoDetect.booleanValue()) {
                log.debug("Auto-detection is enabled, performing auto-detection");
                this.doAutoDetection();
                log.debug("Base auto-detection completed");

                // Auto-detection with dependencies
                this.support.processAutoDetectionWithDependencies(this::doAutoDetectionWithDependency);
            } else {
                log.debug("Auto-detection is disabled, skipping auto-detection");
            }

            // Phase 2: Pre-build with dependencies
            this.support.processPreBuildDependencies(this::doPreBuildWithDependency);

            // Phase 3: Build the target object
            log.debug("Building the instance");
            this.built = this.doBuild();
            log.debug("Built instance: {}", this.built);

            // Phase 4: Post-build with dependencies
            this.support.processPostBuildDependencies(this::doPostBuildWithDependency);

            log.trace("Exiting build method");
            return this.built;
        } catch (DslException e) {
            // Logged at debug because this exception is propagated to the
            // caller — which may legitimately retry (BuilderDependency.tryResolve
            // catches and treats the build as "not yet ready") or fail. Top-
            // level callers (ApiBuilder, user main, etc.) own the decision to
            // surface the error to the user; the framework should not
            // pre-emptively spam ERROR for what is often a transient state
            // during Bootstrap's Phase 1 dependency resolution.
            log.debug("Build failed, propagating to caller: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Performs auto-detection enhanced with a ready dependency.
     * This method is called for each ready dependency during the auto-detection phase.
     *
     * @param dependency the resolved dependency object
     * @throws DslException if an error occurs during auto-detection with dependency
     */
    protected abstract void doAutoDetectionWithDependency(Object dependency) throws DslException;

    /**
     * Processes a dependency before building the target object.
     * This method is called for each ready dependency during the pre-build phase.
     *
     * @param dependency the resolved dependency object
     */
    protected abstract void doPreBuildWithDependency(Object dependency);

    /**
     * Processes a dependency after building the target object.
     * This method is called for each ready dependency during the post-build phase.
     *
     * @param dependency the resolved dependency object
     */
    protected abstract void doPostBuildWithDependency(Object dependency);

    // --- Builder-kind hooks (DependencyKind.BUILDER) ------------------------
    // Fire when the consumer declared a DependencySpec with kind=BUILDER for
    // the matching stage. Default no-op so existing concrete builders need
    // no migration. Override to receive the upstream BUILDER reference
    // (before/after its own build, depending on the stage).

    /**
     * Configure-stage hook receiving the upstream BUILDER reference. Fires
     * during {@link DependencyStage#CONFIGURATION} on each declared
     * {@link DependencyKind#BUILDER} dependency, before any builder build()
     * has run. The hook MUST be idempotent.
     */
    protected void doConfigureWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) throws DslException {
        // no-op by default
    }

    /**
     * Orchestrator entry-point for the {@link DependencyStage#CONFIGURATION
     * CONFIGURATION} stage. Delegates to the internal
     * {@link DependentBuilderSupport}, which iterates each declared
     * CONFIGURATION-stage dep and fires {@link #doConfigureWithDependencyBuilder}
     * exactly once per Bootstrap lifetime.
     */
    @Override
    public void runConfigurationStage() throws DslException {
        this.support.processConfigurationDependencies(this::doConfigureWithDependencyBuilder);
    }

    /**
     * Auto-detect-stage hook receiving the upstream BUILDER reference.
     */
    protected void doAutoDetectionWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) throws DslException {
        // no-op by default
    }

    /**
     * Pre-build-stage hook receiving the upstream BUILDER reference.
     */
    protected void doPreBuildWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) {
        // no-op by default
    }

    /**
     * Post-build-stage hook receiving the upstream BUILDER reference.
     */
    protected void doPostBuildWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) {
        // no-op by default
    }
}
