package com.garganttua.core.dsl.dependency;

import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Base abstract builder providing common dependency management functionality.
 *
 * <p>
 * This class serves as the foundation for builders that need to track and manage
 * dependencies on other observable builders. It implements {@link IDependentBuilder}
 * to provide dependency declaration and tracking through {@code provide()},
 * {@code use()}, and {@code require()} methods.
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
 * <h2>Build Process</h2>
 * <p>
 * The build process follows this sequence:
 * </p>
 * <ol>
 *   <li>Pre-build: Process ready dependencies via {@code doPreBuildWithDependency()}</li>
 *   <li>Build: Execute {@code doBuild()} to create the target object</li>
 *   <li>Post-build: Process ready dependencies via {@code doPostBuildWithDependency()}</li>
 * </ol>
 *
 * @param <B> the concrete builder type for method chaining
 * @param <T> the type of object this builder constructs
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public abstract class AbstractDependentBuilder<B extends IBuilder<T>, T>
        implements IDependentBuilder<B, T> {

    protected final DependentBuilderSupport support;
    protected T built;

    /**
     * Constructs a new AbstractDependentBuilder with specified dependency sets.
     *
     * @param useDependencies    the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if either parameter is null
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    protected AbstractDependentBuilder(
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        log.atTrace().log("Entering AbstractDependentBuilder constructor");
        this.support = new DependentBuilderSupport(useDependencies, requireDependencies);
        log.atTrace().log("Exiting AbstractDependentBuilder constructor");
    }

    protected AbstractDependentBuilder(
            Set<DependencySpec> dependencies) {
        log.atTrace().log("Entering AbstractDependentBuilder constructor");
        this.support = new DependentBuilderSupport(dependencies);
        log.atTrace().log("Exiting AbstractDependentBuilder constructor");
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
            log.atInfo().log("Building the instance");
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
     * This method is called during the build phase after pre-build dependencies are processed.
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
