package com.garganttua.core.dsl;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    protected final Set<IBuilderDependency<?, ?>> useDependencies;
    protected final Set<IBuilderDependency<?, ?>> requireDependencies;
    protected T built;

    /**
     * Constructs a new AbstractDependentBuilder with specified dependency sets.
     *
     * @param useDependencies    the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if either parameter is null
     */
    protected AbstractDependentBuilder(
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        log.atTrace().log("Entering AbstractDependentBuilder constructor");
        Objects.requireNonNull(useDependencies, "Use dependency set cannot be null");
        Objects.requireNonNull(requireDependencies, "Require dependency set cannot be null");

        this.useDependencies = useDependencies.stream()
            .map(BuilderDependency::new)
            .collect(Collectors.toSet());
        
        this.requireDependencies = requireDependencies.stream()
            .map(BuilderDependency::new)
            .collect(Collectors.toSet());

        log.atDebug().log("AbstractDependentBuilder initialized with {} use dependencies and {} require dependencies",
            this.useDependencies.size(), this.requireDependencies.size());
        log.atTrace().log("Exiting AbstractDependentBuilder constructor");
    }

    @SuppressWarnings("unchecked")
    @Override
    public B provide(IObservableBuilder<?, ?> dependency) {
        log.atTrace().log("Entering provide() with dependency: {}", dependency);
        Objects.requireNonNull(dependency, "Dependency cannot be null");

        boolean provided = provideToDependencySet(dependency, this.useDependencies)
            || provideToDependencySet(dependency, this.requireDependencies);

        if (!provided) {
            log.atWarn().log("Provided dependency {} does not match any declared dependencies",
                dependency.getClass().getName());
        }

        log.atTrace().log("Exiting provide()");
        return (B) this;
    }

    /**
     * Attempts to provide a dependency to a specific dependency set.
     *
     * @param dependency    the dependency to provide
     * @param dependencySet the set to search for matching dependencies
     * @return true if a matching dependency was found and provided, false otherwise
     */
    private boolean provideToDependencySet(
            IObservableBuilder<?, ?> dependency,
            Set<IBuilderDependency<?, ?>> dependencySet) {
        
        Optional<IBuilderDependency<?, ?>> foundDep = dependencySet.stream()
            .filter(d -> d.getDependency().isAssignableFrom(dependency.getClass()))
            .findFirst();

        foundDep.ifPresent(d -> {
            ((BuilderDependency<?, ?>) d).handle(dependency);
            log.atDebug().log("Dependency {} successfully provided", dependency.getClass().getName());
        });

        return foundDep.isPresent();
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
        log.atTrace().log("Entering use()");
        Set<Class<? extends IObservableBuilder<?, ?>>> result = useDependencies.stream()
            .map(IBuilderDependency::getDependency)
            .collect(Collectors.toSet());
        log.atDebug().log("Returning {} use dependencies", result.size());
        log.atTrace().log("Exiting use()");
        return result;
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
        log.atTrace().log("Entering require()");
        Set<Class<? extends IObservableBuilder<?, ?>>> result = requireDependencies.stream()
            .map(IBuilderDependency::getDependency)
            .collect(Collectors.toSet());
        log.atDebug().log("Returning {} require dependencies", result.size());
        log.atTrace().log("Exiting require()");
        return result;
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
            processPreBuildDependencies();

            // Phase 2: Build the target object
            log.atInfo().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);

            // Phase 3: Post-build with dependencies
            processPostBuildDependencies();

            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
            throw e;
        }
    }

    /**
     * Processes all dependencies during the pre-build phase.
     * Use dependencies are processed if ready, require dependencies must be ready.
     *
     * @throws DslException if a required dependency is not ready
     */
    private void processPreBuildDependencies() throws DslException {
        log.atTrace().log("Processing pre-build dependencies");

        // Process optional use dependencies
        useDependencies.forEach(d -> d.ifReady(this::doPreBuildWithDependency));

        // Process and validate required dependencies
        for (IBuilderDependency<?, ?> dep : requireDependencies) {
            validateRequiredDependency(dep, "pre-build");
            dep.ifReady(this::doPreBuildWithDependency);
        }

        log.atDebug().log("Pre-build dependency processing completed");
    }

    /**
     * Processes all dependencies during the post-build phase.
     * Use dependencies are processed if ready, require dependencies must be ready.
     *
     * @throws DslException if a required dependency is not ready
     */
    private void processPostBuildDependencies() throws DslException {
        log.atTrace().log("Processing post-build dependencies");

        // Process optional use dependencies
        useDependencies.forEach(d -> d.ifReady(this::doPostBuildWithDependency));

        // Process and validate required dependencies
        for (IBuilderDependency<?, ?> dep : requireDependencies) {
            validateRequiredDependency(dep, "post-build");
            dep.ifReady(this::doPostBuildWithDependency);
        }

        log.atDebug().log("Post-build dependency processing completed");
    }

    /**
     * Validates that a required dependency is either empty or ready.
     *
     * @param dependency the dependency to validate
     * @param phase      the build phase (for error messages)
     * @throws DslException if the dependency is neither empty nor ready
     */
    private void validateRequiredDependency(IBuilderDependency<?, ?> dependency, String phase) throws DslException {
        if (!dependency.isEmpty() && !dependency.isReady()) {
            String errorMsg = String.format(
                "Required dependency %s is not ready during %s phase",
                dependency.getDependency().getSimpleName(),
                phase
            );
            log.atError().log(errorMsg);
            throw new DslException(errorMsg);
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
