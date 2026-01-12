package com.garganttua.core.dsl.dependency;

import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public abstract class AbstractAutomaticDependentBuilder<B extends IBuilder<T>, T>
        extends AbstractAutomaticBuilder<B, T>
        implements IDependentBuilder<B, T> {

    protected final DependentBuilderSupport support;

    /**
     * Constructs a new AbstractAutomaticDependentBuilder with specified dependency sets.
     *
     * @param useDependencies    the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if either parameter is null
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    protected AbstractAutomaticDependentBuilder(
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        super();
        log.atTrace().log("Entering AbstractAutomaticDependentBuilder constructor");
        this.support = new DependentBuilderSupport(useDependencies, requireDependencies);
        log.atTrace().log("Exiting AbstractAutomaticDependentBuilder constructor");
    }

    protected AbstractAutomaticDependentBuilder(
            Set<DependencySpec> dependencies) {
        super();
        log.atTrace().log("Entering AbstractAutomaticDependentBuilder constructor");
        this.support = new DependentBuilderSupport(dependencies);
        log.atTrace().log("Exiting AbstractAutomaticDependentBuilder constructor");
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
            // Phase 1: Auto-detection
            if (this.autoDetect) {
                log.atInfo().log("Auto-detection is enabled, performing auto-detection");
                this.doAutoDetection();
                log.atDebug().log("Base auto-detection completed");

                // Auto-detection with dependencies
                this.support.processAutoDetectionWithDependencies(this::doAutoDetectionWithDependency);
            } else {
                log.atDebug().log("Auto-detection is disabled, skipping auto-detection");
            }

            // Phase 2: Pre-build with dependencies
            this.support.processPreBuildDependencies(this::doPreBuildWithDependency);

            // Phase 3: Build the target object
            log.atInfo().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);

            // Phase 4: Post-build with dependencies
            this.support.processPostBuildDependencies(this::doPostBuildWithDependency);

            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
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
}
