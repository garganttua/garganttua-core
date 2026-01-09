package com.garganttua.core.dsl;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    protected final Set<IBuilderDependency<?, ?>> useDependencies;
    protected final Set<IBuilderDependency<?, ?>> requireDependencies;

    /**
     * Constructs a new AbstractAutomaticDependentBuilder with specified dependency sets.
     *
     * @param useDependencies    the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if either parameter is null
     */
    protected AbstractAutomaticDependentBuilder(
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        super();
        log.atTrace().log("Entering AbstractAutomaticDependentBuilder constructor");
        Objects.requireNonNull(useDependencies, "Use dependency set cannot be null");
        Objects.requireNonNull(requireDependencies, "Require dependency set cannot be null");

        this.useDependencies = useDependencies.stream()
            .map(BuilderDependency::new)
            .collect(Collectors.toSet());
        
        this.requireDependencies = requireDependencies.stream()
            .map(BuilderDependency::new)
            .collect(Collectors.toSet());

        log.atDebug().log("AbstractAutomaticDependentBuilder initialized with {} use and {} require dependencies",
            this.useDependencies.size(), this.requireDependencies.size());
        log.atTrace().log("Exiting AbstractAutomaticDependentBuilder constructor");
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
            // Phase 1: Auto-detection
            if (this.autoDetect) {
                log.atInfo().log("Auto-detection is enabled, performing auto-detection");
                this.doAutoDetection();
                log.atDebug().log("Base auto-detection completed");

                // Auto-detection with dependencies
                processAutoDetectionWithDependencies();
            } else {
                log.atDebug().log("Auto-detection is disabled, skipping auto-detection");
            }

            // Phase 2: Pre-build with dependencies
            processPreBuildDependencies();

            // Phase 3: Build the target object
            log.atInfo().log("Building the instance");
            this.built = this.doBuild();
            log.atDebug().log("Built instance: {}", this.built);

            // Phase 4: Post-build with dependencies
            processPostBuildDependencies();

            log.atTrace().log("Exiting build method");
            return this.built;
        } catch (DslException e) {
            log.atError().log("Critical error during build", e);
            throw e;
        }
    }

    /**
     * Processes auto-detection with all ready dependencies.
     * This is called after base auto-detection if auto-detect is enabled.
     *
     * @throws DslException if an error occurs during auto-detection with dependencies
     */
    private void processAutoDetectionWithDependencies() throws DslException {
        log.atTrace().log("Processing auto-detection with dependencies");

        // Process optional use dependencies
        useDependencies.forEach(d -> d.ifReady(this::doAutoDetectionWithDependency));

        // Process required dependencies
        for (IBuilderDependency<?, ?> dep : requireDependencies) {
            // No validation here - required dependencies may not be provided yet during auto-detection
            dep.ifReady(this::doAutoDetectionWithDependency);
        }

        log.atDebug().log("Auto-detection with dependencies completed");
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
