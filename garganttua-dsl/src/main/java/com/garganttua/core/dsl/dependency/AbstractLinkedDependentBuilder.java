package com.garganttua.core.dsl.dependency;

import java.util.Set;

import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public abstract class AbstractLinkedDependentBuilder<B extends IBuilder<T>, L, T>
        extends AbstractLinkedBuilder<L, T>
        implements IDependentBuilder<B, T> {

    protected final DependentBuilderSupport support;
    protected T built;

    /**
     * Constructs a new AbstractLinkedDependentBuilder with specified link and
     * dependency sets.
     *
     * @param link                the parent builder to link to
     * @param useDependencies     the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if any parameter is null
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    protected AbstractLinkedDependentBuilder(
            L link,
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        super(link);
        log.atTrace().log("Entering AbstractLinkedDependentBuilder constructor");
        this.support = new DependentBuilderSupport(useDependencies, requireDependencies);
        log.atTrace().log("Exiting AbstractLinkedDependentBuilder constructor");
    }

    protected AbstractLinkedDependentBuilder(
            L link,
            Set<DependencySpec> dependencies) {
        super(link);
        log.atTrace().log("Entering AbstractLinkedDependentBuilder constructor");
        this.support = new DependentBuilderSupport(dependencies);
        log.atTrace().log("Exiting AbstractLinkedDependentBuilder constructor");
    }

    @SuppressWarnings("unchecked")
    @Override
    public B provide(IObservableBuilder<?, ?> dependency) throws DslException {
        this.support.provide(dependency);
        return (B) this;
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
        return this.support.use();
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
        return this.support.require();
    }

    @Override
    public T build() throws DslException {
        log.atTrace().log("Entering build method");

        if (this.built != null) {
            log.atDebug().log("Returning previously built instance: {}", this.built);
            log.atTrace().log("Exiting build method (cached)");
            return this.built;
        }

        try {
            // Phase 1: Pre-build with dependencies
            this.support.processPreBuildDependencies(this::doPreBuildWithDependency);

            // Phase 2: Build the target object
            log.atDebug().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);

            // Phase 3: Post-build with dependencies
            this.support.processPostBuildDependencies(this::doPostBuildWithDependency);

            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
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
}
