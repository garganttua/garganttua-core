package com.garganttua.core.dsl.dependency;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * Support class providing common dependency management functionality for
 * dependent builders.
 *
 * <p>
 * This class encapsulates all the common logic for managing dependencies in
 * builders
 * implementing {@link IDependentBuilder}. It provides methods for dependency
 * provision,
 * validation, and processing during different build phases.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * Dependent builder implementations should create an instance of this class in
 * their
 * constructor and delegate dependency management operations to it.
 * </p>
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
 * <h3>Processing Methods</h3>
 * <ul>
 *   <li>{@link #processAutoDetectionWithDependencies(Consumer)} - Process dependencies during auto-detection phase</li>
 *   <li>{@link #processPreBuildDependencies(Consumer)} - Process dependencies before build phase</li>
 *   <li>{@link #processPostBuildDependencies(Consumer)} - Process dependencies after build phase</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see IDependentBuilder
 * @see BuilderDependency
 */
@Slf4j
public class DependentBuilderSupport {

    protected final Set<IBuilderDependency<?, ?>> useDependencies;
    protected final Set<IBuilderDependency<?, ?>> requireDependencies;
    protected final Set<IBuilderDependency<?, ?>> allDependencies;

    /**
     * Constructs a new DependentBuilderSupport with specified dependency sets.
     *
     * @param useDependencies     the set of optional dependency classes
     * @param requireDependencies the set of required dependency classes
     * @throws NullPointerException if either parameter is null
     * @deprecated Use {@link #DependentBuilderSupport(Set)} with DependencySpec
     *             instead
     */
    @Deprecated(since = "2.0.0-ALPHA01", forRemoval = true)
    public DependentBuilderSupport(
            Set<Class<? extends IObservableBuilder<?, ?>>> useDependencies,
            Set<Class<? extends IObservableBuilder<?, ?>>> requireDependencies) {
        log.atTrace().log("Entering DependentBuilderSupport constructor (deprecated)");
        Objects.requireNonNull(useDependencies, "Use dependency set cannot be null");
        Objects.requireNonNull(requireDependencies, "Require dependency set cannot be null");

        this.useDependencies = useDependencies.stream()
                .map(BuilderDependency::new)
                .collect(Collectors.toSet());

        this.requireDependencies = requireDependencies.stream()
                .map(BuilderDependency::new)
                .collect(Collectors.toSet());

        this.allDependencies = new HashSet<>();
        this.allDependencies.addAll(this.useDependencies);
        this.allDependencies.addAll(this.requireDependencies);

        log.atDebug().log("DependentBuilderSupport initialized with {} use and {} require dependencies",
                this.useDependencies.size(), this.requireDependencies.size());
        log.atTrace().log("Exiting DependentBuilderSupport constructor (deprecated)");
    }

    /**
     * Constructs a new DependentBuilderSupport with phase-aware dependency
     * specifications.
     *
     * <p>
     * With phase-aware dependencies, a single dependency can be required in one
     * phase
     * and optional in another. Therefore, all dependencies are stored in a unified
     * collection and their requirement level is checked per-phase.
     * </p>
     *
     * @param dependencySpecs the set of dependency specifications
     * @throws NullPointerException if dependencySpecs is null
     */
    public DependentBuilderSupport(Set<DependencySpec> dependencySpecs) {
        log.atTrace().log("Entering DependentBuilderSupport constructor with DependencySpec");
        Objects.requireNonNull(dependencySpecs, "Dependency specifications cannot be null");

        // Create all dependencies
        this.allDependencies = dependencySpecs.stream()
                .map(spec -> new BuilderDependency<>(spec.dependencyBuilderClass(), spec))
                .collect(Collectors.toSet());

        // For backward compatibility with use() and require() methods,
        // classify dependencies based on overall requirement
        // (A dependency is "required" if it's required in ANY phase)
        this.useDependencies = this.allDependencies.stream()
                .filter(d -> d instanceof BuilderDependency<?, ?> bd &&
                        !bd.isRequiredForAutoDetect() && !bd.isRequiredForBuild())
                .collect(Collectors.toSet());

        this.requireDependencies = this.allDependencies.stream()
                .filter(d -> d instanceof BuilderDependency<?, ?> bd &&
                        (bd.isRequiredForAutoDetect() || bd.isRequiredForBuild()))
                .collect(Collectors.toSet());

        log.atDebug().log(
                "DependentBuilderSupport initialized with {} total dependencies ({} use, {} require) from {} specs",
                this.allDependencies.size(), this.useDependencies.size(), this.requireDependencies.size(),
                dependencySpecs.size());
        log.atTrace().log("Exiting DependentBuilderSupport constructor with DependencySpec");
    }

    /**
     * Provides a dependency to the builder.
     *
     * @param dependency         the dependency to provide
     * @param dependencyProvider functional interface to get all expected
     *                           dependencies
     * @throws NullPointerException if dependency is null
     * @throws DslException         if the dependency is not in the expected
     *                              dependencies list
     */
    public void provide(IObservableBuilder<?, ?> dependency) throws DslException {
        log.atTrace().log("Entering provide() with dependency: {}", dependency);
        Objects.requireNonNull(dependency, "Dependency cannot be null");

        // Validate that the provided dependency is in the expected dependencies list
        if (!isExpectedDependency(dependency)) {
            String errorMsg = String.format(
                    "Provided dependency %s is not declared in the expected dependencies list",
                    dependency.getClass().getName());
            log.atError().log(errorMsg);
            throw new DslException(errorMsg);
        }

        boolean provided = provideToDependencySet(dependency, this.useDependencies)
                || provideToDependencySet(dependency, this.requireDependencies);

        if (!provided) {
            log.atWarn().log("Provided dependency {} does not match any declared dependencies",
                    dependency.getClass().getName());
        }

        log.atTrace().log("Exiting provide()");
    }

    /**
     * Checks if the provided dependency is expected by consulting the
     * dependencies() method.
     *
     * @param dependency         the dependency to validate
     * @param dependencyProvider functional interface to get all expected
     *                           dependencies
     * @return true if the dependency is expected, false otherwise
     */
    private boolean isExpectedDependency(
            IObservableBuilder<?, ?> dependency) {
        Set<Class<? extends IObservableBuilder<?, ?>>> expectedDependencies = dependencies();
        return expectedDependencies.stream()
                .anyMatch(expectedClass -> expectedClass.isAssignableFrom(dependency.getClass()));
    }

    private Set<Class<? extends IObservableBuilder<?, ?>>> dependencies() {
        Set<Class<? extends IObservableBuilder<?, ?>>> deps = new HashSet<>();
        deps.addAll(this.use());
        deps.addAll(this.require());
        return deps;
    }

    /**
     * Attempts to provide a dependency builder to a specific dependency set.
     *
     * @param dependency    the dependency builder to provide
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

    /**
     * Returns the set of optional use dependencies.
     *
     * @return the set of use dependency classes
     */
    public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
        log.atTrace().log("Entering use()");
        Set<Class<? extends IObservableBuilder<?, ?>>> result = useDependencies.stream()
                .map(IBuilderDependency::getDependency)
                .collect(Collectors.toSet());
        log.atDebug().log("Returning {} use dependencies", result.size());
        log.atTrace().log("Exiting use()");
        return result;
    }

    /**
     * Returns the set of required dependencies.
     *
     * @return the set of require dependency classes
     */
    public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
        log.atTrace().log("Entering require()");
        Set<Class<? extends IObservableBuilder<?, ?>>> result = requireDependencies.stream()
                .map(IBuilderDependency::getDependency)
                .collect(Collectors.toSet());
        log.atDebug().log("Returning {} require dependencies", result.size());
        log.atTrace().log("Exiting require()");
        return result;
    }

    /**
     * Returns the internal set of use dependency trackers.
     * This is provided for advanced use cases where direct access to dependency
     * state is needed.
     *
     * @return the set of use dependency trackers
     */
    public Set<IBuilderDependency<?, ?>> getUseDependencies() {
        return useDependencies;
    }

    /**
     * Returns the internal set of require dependency trackers.
     * This is provided for advanced use cases where direct access to dependency
     * state is needed.
     *
     * @return the set of require dependency trackers
     */
    public Set<IBuilderDependency<?, ?>> getRequireDependencies() {
        return requireDependencies;
    }

    /**
     * Processes all dependencies during the pre-build phase.
     * Only dependencies needed for BUILD phase are processed.
     *
     * <p>Validation rules:</p>
     * <ul>
     *   <li>For "use" (optional) dependencies: if provide() was called, the builder must be built</li>
     *   <li>For "require" (required) dependencies: must be provided AND built</li>
     * </ul>
     *
     * @param preBuildHandler the handler to call for each ready dependency
     * @throws DslException if validation fails for any dependency
     */
    public void processPreBuildDependencies(Consumer<Object> preBuildHandler) throws DslException {
        log.atTrace().log("Processing pre-build dependencies");

        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (dep instanceof BuilderDependency<?, ?> bd && bd.isNeededForBuild()) {
                // Validate based on requirement level
                if (bd.isRequiredForBuild()) {
                    // Required: must be provided and built
                    bd.validateRequiredDependency("BUILD");
                } else if (bd.isOptionalForBuild()) {
                    // Optional: if provided, must be built
                    bd.validateUseDependency();
                }

                // Process if ready (both required and optional)
                processIfReady(dep, preBuildHandler);
            }
        }

        log.atDebug().log("Pre-build dependency processing completed");
    }

    /**
     * Processes all dependencies during the post-build phase.
     * Only dependencies needed for BUILD phase are processed.
     *
     * <p>Validation rules:</p>
     * <ul>
     *   <li>For "use" (optional) dependencies: if provide() was called, the builder must be built</li>
     *   <li>For "require" (required) dependencies: must be provided AND built</li>
     * </ul>
     *
     * @param postBuildHandler the handler to call for each ready dependency
     * @throws DslException if validation fails for any dependency
     */
    public void processPostBuildDependencies(Consumer<Object> postBuildHandler) throws DslException {
        log.atTrace().log("Processing post-build dependencies");

        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (dep instanceof BuilderDependency<?, ?> bd && bd.isNeededForBuild()) {
                // Validate based on requirement level
                if (bd.isRequiredForBuild()) {
                    // Required: must be provided and built
                    bd.validateRequiredDependency("BUILD");
                } else if (bd.isOptionalForBuild()) {
                    // Optional: if provided, must be built
                    bd.validateUseDependency();
                }

                // Process if ready (both required and optional)
                processIfReady(dep, postBuildHandler);
            }
        }

        log.atDebug().log("Post-build dependency processing completed");
    }

    /**
     * Processes auto-detection with all ready dependencies.
     * Only dependencies needed for AUTO_DETECT phase are processed.
     *
     * <p>Validation rules:</p>
     * <ul>
     *   <li>For "use" (optional) dependencies: if provide() was called, the builder must be built</li>
     *   <li>For "require" (required) dependencies: must be provided AND built</li>
     * </ul>
     *
     * @param autoDetectHandler the handler to call for each ready dependency
     * @throws DslException if validation fails for any dependency
     */
    public void processAutoDetectionWithDependencies(Consumer<Object> autoDetectHandler) throws DslException {
        log.atTrace().log("Processing auto-detection with dependencies");

        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (dep instanceof BuilderDependency<?, ?> bd && bd.isNeededForAutoDetect()) {
                // Validate based on requirement level
                if (bd.isRequiredForAutoDetect()) {
                    // Required: must be provided and built
                    bd.validateRequiredDependency("AUTO_DETECT");
                } else if (bd.isOptionalForAutoDetect()) {
                    // Optional: if provided, must be built
                    bd.validateUseDependency();
                }

                // Process if ready (both required and optional)
                processIfReady(dep, autoDetectHandler);
            }
        }

        log.atDebug().log("Auto-detection with dependencies completed");
    }

    /**
     * Helper method to process a dependency if ready, handling generic type
     * capture.
     *
     * @param dependency the dependency to process
     * @param handler    the handler to call with the dependency's built object
     */
    @SuppressWarnings("unchecked")
    private void processIfReady(IBuilderDependency<?, ?> dependency, Consumer<Object> handler) {
        dependency.ifReady((Consumer) handler);
    }
}
