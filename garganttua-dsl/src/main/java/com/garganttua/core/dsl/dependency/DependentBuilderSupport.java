package com.garganttua.core.dsl.dependency;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.reflection.IClass;

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
public class DependentBuilderSupport {
    private static final Logger log = Logger.getLogger(DependentBuilderSupport.class);

    protected final Set<IBuilderDependency<?, ?>> useDependencies;
    protected final Set<IBuilderDependency<?, ?>> requireDependencies;
    protected final Set<IBuilderDependency<?, ?>> allDependencies;

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
        log.trace("Entering DependentBuilderSupport constructor with DependencySpec");
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

        log.debug(
                "DependentBuilderSupport initialized with {} total dependencies ({} use, {} require) from {} specs",
                this.allDependencies.size(), this.useDependencies.size(), this.requireDependencies.size(),
                dependencySpecs.size());
        log.trace("Exiting DependentBuilderSupport constructor with DependencySpec");
    }

    /**
     * Provides a dependency to the builder.
     *
     * @param dependency         the dependency to provide
     * @throws NullPointerException if dependency is null
     * @throws DslException         if the dependency is not in the expected
     *                              dependencies list
     */
    public void provide(IObservableBuilder<?, ?> dependency) throws DslException {
        log.trace("Entering provide() with dependency: {}", dependency);
        Objects.requireNonNull(dependency, "Dependency cannot be null");

        // Validate that the provided dependency is in the expected dependencies list
        if (!isExpectedDependency(dependency)) {
            String errorMsg = String.format(
                    "Provided dependency %s is not declared in the expected dependencies list",
                    dependency.getClass().getName());
            log.error(errorMsg);
            throw new DslException(errorMsg);
        }

        // Hand the builder ref to EVERY matching BuilderDependency. A
        // consumer may declare the same upstream class multiple times across
        // different stages (e.g. CONFIGURATION + BUILD) — every one of those
        // deps must receive the reference, otherwise the un-fed ones will
        // throw "not provided" at validation time.
        int matched = 0;
        for (IBuilderDependency<?, ?> dep : this.allDependencies) {
            if (dep.getDependency().isAssignableFrom(dependency.getClass())) {
                ((BuilderDependency<?, ?>) dep).handle(dependency);
                matched++;
            }
        }
        if (matched == 0) {
            log.warn("Provided dependency {} does not match any declared dependencies",
                    dependency.getClass().getName());
        } else {
            log.debug("Dependency {} fed to {} declared spec(s)",
                    dependency.getClass().getName(), matched);
        }

        log.trace("Exiting provide()");
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
        Set<IClass<? extends IObservableBuilder<?, ?>>> expectedDependencies = dependencies();
        return expectedDependencies.stream()
                .anyMatch(expectedClass -> expectedClass.isAssignableFrom(dependency.getClass()));
    }

    private Set<IClass<? extends IObservableBuilder<?, ?>>> dependencies() {
        Set<IClass<? extends IObservableBuilder<?, ?>>> deps = new HashSet<>();
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
            log.debug("Dependency {} successfully provided", dependency.getClass().getName());
        });

        return foundDep.isPresent();
    }

    /**
     * Returns the set of optional use dependencies.
     *
     * @return the set of use dependency classes
     */
    public Set<IClass<? extends IObservableBuilder<?, ?>>> use() {
        log.trace("Entering use()");
        Set<IClass<? extends IObservableBuilder<?, ?>>> result = useDependencies.stream()
                .map(IBuilderDependency::getDependency)
                .collect(Collectors.toSet());
        log.debug("Returning {} use dependencies", result.size());
        log.trace("Exiting use()");
        return result;
    }

    /**
     * Returns the set of required dependencies.
     *
     * @return the set of require dependency classes
     */
    public Set<IClass<? extends IObservableBuilder<?, ?>>> require() {
        log.trace("Entering require()");
        Set<IClass<? extends IObservableBuilder<?, ?>>> result = requireDependencies.stream()
                .map(IBuilderDependency::getDependency)
                .collect(Collectors.toSet());
        log.debug("Returning {} require dependencies", result.size());
        log.trace("Exiting require()");
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
        log.trace("Processing pre-build dependencies (BUILT kind)");

        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) {
                continue;
            }
            // Only BUILD-stage BUILT-kind deps fire here; CONFIGURATION /
            // AUTO_DETECT / BUILDER-kind deps go through their dedicated
            // iterators.
            if (!bd.isBuildStage() || !bd.isBuiltKind()) {
                continue;
            }
            if (bd.spec().isRequired()) {
                bd.validateRequiredDependency("BUILD");
            } else {
                bd.validateUseDependency();
            }
            if (bd.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD)) {
                processIfReady(dep, preBuildHandler);
            }
        }

        log.debug("Pre-build dependency processing completed");
    }

    /**
     * Iterate BUILD-stage BUILDER-kind dependencies and hand the
     * <strong>builder</strong> reference to the supplied handler. Idempotent:
     * each {@code (consumer, dep)} pair fires at most once per Bootstrap
     * lifetime.
     */
    public void processPreBuildDependencyBuilders(
            Consumer<IObservableBuilder<?, ?>> handler) throws DslException {
        log.trace("Processing pre-build dependencies (BUILDER kind)");
        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) continue;
            if (!bd.isBuildStage() || !bd.isBuilderKind()) continue;
            if (bd.isRequired() && !bd.hasBuilder()) {
                bd.validateRequiredDependency("BUILD");   // throws
            }
            if (bd.hasBuilder()
                    && bd.tryMarkFired(BuilderDependency.FiringEvent.PRE_BUILD)) {
                handler.accept(bd.builder());
            }
        }
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
        log.trace("Processing post-build dependencies (BUILT kind)");

        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) continue;
            if (!bd.isBuildStage() || !bd.isBuiltKind()) continue;
            if (bd.isRequired()) {
                bd.validateRequiredDependency("BUILD");
            } else {
                bd.validateUseDependency();
            }
            if (bd.tryMarkFired(BuilderDependency.FiringEvent.POST_BUILD)) {
                processIfReady(dep, postBuildHandler);
            }
        }

        log.debug("Post-build dependency processing completed");
    }

    /**
     * BUILDER-kind variant of {@link #processPostBuildDependencies}.
     */
    public void processPostBuildDependencyBuilders(
            Consumer<IObservableBuilder<?, ?>> handler) throws DslException {
        log.trace("Processing post-build dependencies (BUILDER kind)");
        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) continue;
            if (!bd.isBuildStage() || !bd.isBuilderKind()) continue;
            if (bd.isRequired() && !bd.hasBuilder()) {
                bd.validateRequiredDependency("BUILD");
            }
            if (bd.hasBuilder()
                    && bd.tryMarkFired(BuilderDependency.FiringEvent.POST_BUILD)) {
                handler.accept(bd.builder());
            }
        }
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
        log.trace("Processing auto-detection with dependencies (BUILT kind)");

        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) continue;
            if (!bd.isAutoDetectStage() || !bd.isBuiltKind()) continue;
            if (bd.isRequired()) {
                bd.validateRequiredDependency("AUTO_DETECT");
            } else {
                bd.validateUseDependency();
            }
            if (bd.tryMarkFired(BuilderDependency.FiringEvent.AUTO_DETECT)) {
                processIfReady(dep, autoDetectHandler);
            }
        }

        log.debug("Auto-detection with dependencies completed");
    }

    /**
     * BUILDER-kind variant of {@link #processAutoDetectionWithDependencies}.
     */
    public void processAutoDetectionWithDependencyBuilders(
            Consumer<IObservableBuilder<?, ?>> handler) throws DslException {
        log.trace("Processing auto-detection with dependencies (BUILDER kind)");
        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) continue;
            if (!bd.isAutoDetectStage() || !bd.isBuilderKind()) continue;
            if (bd.isRequired() && !bd.hasBuilder()) {
                bd.validateRequiredDependency("AUTO_DETECT");
            }
            if (bd.hasBuilder()
                    && bd.tryMarkFired(BuilderDependency.FiringEvent.AUTO_DETECT)) {
                handler.accept(bd.builder());
            }
        }
    }

    /**
     * Iterate {@link DependencyStage#CONFIGURATION CONFIGURATION}-stage
     * dependencies. Always {@link DependencyKind#BUILDER} since no built
     * object exists at configuration time. Idempotent.
     */
    public void processConfigurationDependencies(
            Consumer<IObservableBuilder<?, ?>> handler) throws DslException {
        log.trace("Processing configuration-stage dependencies");
        for (IBuilderDependency<?, ?> dep : allDependencies) {
            if (!(dep instanceof BuilderDependency<?, ?> bd)) continue;
            if (!bd.isConfigurationStage()) continue;
            if (bd.isRequired() && !bd.hasBuilder()) {
                bd.validateRequiredDependency("CONFIGURATION");
            }
            if (bd.hasBuilder()
                    && bd.tryMarkFired(BuilderDependency.FiringEvent.CONFIGURATION)) {
                handler.accept(bd.builder());
            }
        }
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
