package com.garganttua.core.dsl.dependency;

import java.util.Objects;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Abstract builder combining automatic detection, linked navigation, and dependency management.
 *
 * <p>
 * This class extends {@link AbstractAutomaticBuilder} and implements both {@link ILinkedBuilder}
 * and {@link IDependentBuilder} to provide:
 * <ul>
 *   <li>Auto-detection capabilities via {@code autoDetect()}</li>
 *   <li>Linked builder navigation via {@code up()}</li>
 *   <li>Dependency declaration and tracking via {@code provide()}, {@code use()}, and {@code require()}</li>
 * </ul>
 *
 * <p><b>Dependency Lifecycle</b></p>
 * <p>
 * Dependencies are managed through three main phases:
 * </p>
 * <ol>
 *   <li><b>Declaration</b>: Dependencies are declared in the constructor via use/require sets</li>
 *   <li><b>Provision</b>: Concrete dependency instances are provided via {@code provide()}</li>
 *   <li><b>Resolution</b>: During {@code build()}, dependencies are validated and processed</li>
 * </ol>
 *
 * <h2>Build Process with Auto-Detection and Linked Navigation</h2>
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
 * @param <L> the type of the linked parent builder
 * @param <T> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see AbstractAutomaticBuilder
 * @see ILinkedBuilder
 * @see IDependentBuilder
 */
public abstract class AbstractAutomaticLinkedDependentBuilder<B extends IBuilder<T>, L, T>
        extends AbstractAutomaticBuilder<B, T>
        implements IAutomaticLinkedBuilder<B, L, T>, IDependentBuilder<B, T> {
    private static final Logger log = Logger.getLogger(AbstractAutomaticLinkedDependentBuilder.class);

    private L link;
    protected final DependentBuilderSupport support;

    /**
     * Constructs a linked dependent builder with the given parent and dependency
     * specifications, merging any specs declared via annotations on the concrete class.
     * Auto-detection is initially disabled.
     *
     * @param link the parent builder reachable via {@link #up()}
     * @param dependencies the declared dependency specifications
     * @throws NullPointerException if {@code link} is {@code null}
     */
    protected AbstractAutomaticLinkedDependentBuilder(
            L link,
            Set<DependencySpec> dependencies) {
        super();
        log.trace("Entering AbstractAutomaticLinkedDependentBuilder constructor");
        this.link = Objects.requireNonNull(link, "Link cannot be null");
        Set<DependencySpec> merged = new java.util.LinkedHashSet<>(dependencies);
        merged.addAll(DependencySpec.fromAnnotations(this.getClass()));
        this.support = new DependentBuilderSupport(merged);
        this.autoDetect = false;
        log.debug("AbstractAutomaticLinkedDependentBuilder initialized with link, {} dependencies",
            dependencies.size());
        log.trace("Exiting AbstractAutomaticLinkedDependentBuilder constructor");
    }

    /**
     * Returns the parent builder this builder is linked to.
     *
     * @return the parent (link) builder
     */
    @Override
    public L up() {
        log.trace("Entering up() method");
        log.debug("Returning link: {}", this.link);
        log.trace("Exiting up() method");
        return this.link;
    }

    /**
     * Re-parents this builder to a new link.
     *
     * @param link the new parent builder
     * @return this builder for method chaining
     * @throws NullPointerException if {@code link} is {@code null}
     */
    @SuppressWarnings("unchecked")
    @Override
    public B setUp(L link) {
        log.trace("Entering setUp() with link: {}", link);
        this.link = Objects.requireNonNull(link, "Link cannot be null");
        log.debug("Link updated to: {}", this.link);
        log.trace("Exiting setUp()");
        return (B) this;
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

    /**
     * Configure-stage hook receiving the upstream BUILDER reference. No-op by
     * default; override to react to a {@link DependencyKind#BUILDER} dependency
     * during {@link DependencyStage#CONFIGURATION}. Must be idempotent.
     *
     * @param dependencyBuilder the upstream dependency builder
     * @throws DslException if configuration fails
     */
    protected void doConfigureWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) throws DslException {
        // no-op by default
    }

    /**
     * Orchestrator entry-point for the {@link DependencyStage#CONFIGURATION}
     * stage, firing {@link #doConfigureWithDependencyBuilder} for each declared
     * configuration-stage dependency.
     *
     * @throws DslException if a configuration hook fails
     */
    @Override
    public void runConfigurationStage() throws DslException {
        this.support.processConfigurationDependencies(this::doConfigureWithDependencyBuilder);
    }

    /**
     * Auto-detect-stage hook receiving the upstream BUILDER reference. No-op by default.
     *
     * @param dependencyBuilder the upstream dependency builder
     * @throws DslException if auto-detection fails
     */
    protected void doAutoDetectionWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) throws DslException {
        // no-op by default
    }

    /**
     * Pre-build-stage hook receiving the upstream BUILDER reference. No-op by default.
     *
     * @param dependencyBuilder the upstream dependency builder
     */
    protected void doPreBuildWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) {
        // no-op by default
    }

    /**
     * Post-build-stage hook receiving the upstream BUILDER reference. No-op by default.
     *
     * @param dependencyBuilder the upstream dependency builder
     */
    protected void doPostBuildWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder) {
        // no-op by default
    }
}
