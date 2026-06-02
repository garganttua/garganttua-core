package com.garganttua.core.dsl.dependency;

import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Abstract builder combining linked navigation and dependency management.
 *
 * <p>
 * This class extends {@link AbstractLinkedBuilder} and implements
 * {@link IDependentBuilder}
 * to provide both linked builder navigation (via {@code up()}) and the ability
 * to
 * declare and track dependencies on other builders.
 * </p>
 *
 * <h2>Dependency Lifecycle</h2>
 * <p>
 * Dependencies are managed through three main phases:
 * </p>
 * <ol>
 * <li><b>Declaration</b>: Dependencies are declared in the constructor via
 * use/require sets</li>
 * <li><b>Provision</b>: Concrete dependency instances are provided via
 * {@code provide()}</li>
 * <li><b>Resolution</b>: During {@code build()}, dependencies are validated and
 * processed</li>
 * </ol>
 *
 * <h2>Build Process</h2>
 * <p>
 * The build process follows this sequence:
 * </p>
 * <ol>
 * <li>Pre-build: Process ready dependencies via
 * {@code doPreBuildWithDependency()}</li>
 * <li>Build: Execute {@code doBuild()} to create the target object</li>
 * <li>Post-build: Process ready dependencies via
 * {@code doPostBuildWithDependency()}</li>
 * </ol>
 *
 * @param <B> the concrete builder type for method chaining
 * @param <L> the type of the linked parent builder
 * @param <T> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 * @see AbstractLinkedBuilder
 * @see IDependentBuilder
 */
public abstract class AbstractLinkedDependentBuilder<B extends IBuilder<T>, L, T>
        extends AbstractLinkedBuilder<L, T>
        implements IDependentBuilder<B, T> {
    private static final Logger log = Logger.getLogger(AbstractLinkedDependentBuilder.class);

    protected final DependentBuilderSupport support;
    protected T built;

    /**
     * Constructs a linked dependent builder with the given parent and dependency
     * specifications, merging any specs declared via annotations on the concrete class.
     *
     * @param link the parent builder reachable via {@link #up()}
     * @param dependencies the declared dependency specifications
     * @throws NullPointerException if {@code link} is {@code null}
     */
    protected AbstractLinkedDependentBuilder(
            L link,
            Set<DependencySpec> dependencies) {
        super(link);
        log.trace("Entering AbstractLinkedDependentBuilder constructor");
        Set<DependencySpec> merged = new java.util.LinkedHashSet<>(dependencies);
        merged.addAll(DependencySpec.fromAnnotations(this.getClass()));
        this.support = new DependentBuilderSupport(merged);
        log.trace("Exiting AbstractLinkedDependentBuilder constructor");
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
     * Builds the target object, running the pre-build and post-build dependency
     * phases around {@code doBuild()}. The result is cached on first success and
     * returned directly on subsequent calls.
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
            // Phase 1: Pre-build with dependencies
            this.support.processPreBuildDependencies(this::doPreBuildWithDependency);

            // Phase 2: Build the target object
            log.debug("Building the instance");
            this.built = this.doBuild();
            log.debug("Built instance: {}", this.built);

            // Phase 3: Post-build with dependencies
            this.support.processPostBuildDependencies(this::doPostBuildWithDependency);

            log.trace("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.debug("Build failed, propagating to caller: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Builds the target object.
     * This method is called during the build phase after pre-build dependencies are
     * processed.
     *
     * @return the built object
     * @throws DslException if an error occurs during building
     */
    protected abstract T doBuild() throws DslException;

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
