package com.garganttua.core.bootstrap.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.reflection.IAnnotationScanner;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for bootstrapping Garganttua applications.
 *
 * <p>
 * The {@code BootstrapBuilder} provides a fluent API for configuring and
 * initializing
 * a Garganttua application. It manages the lifecycle of various builders
 * (injection context,
 * runtime, expression context, etc.) and orchestrates their initialization in
 * the correct order.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>{@code
 * Object app = new BootstrapBuilder()
 *         .withPackage("com.myapp")
 *         .withBuilder(injectionContextBuilder)
 *         .withBuilder(runtimeBuilder)
 *         .autoDetect(true)
 *         .build();
 * }</pre>
 *
 * <h2>Features</h2>
 * <ul>
 * <li>Automatic package scanning configuration</li>
 * <li>Builder lifecycle management</li>
 * <li>Auto-detection support</li>
 * <li>Dependency resolution between builders</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class BootstrapBuilder extends AbstractAutomaticBuilder<IBoostrap, Object> implements IBoostrap {

    private final Set<String> packages = new HashSet<>();
    private final List<IBuilder<?>> builders = new ArrayList<>();

    /**
     * Creates a new BootstrapBuilder instance.
     *
     * @return a new BootstrapBuilder
     */
    public static IBoostrap builder() {
        log.atTrace().log("Creating new BootstrapBuilder instance");
        return new BootstrapBuilder();
    }

    /**
     * Default constructor.
     */
    public BootstrapBuilder() {
        log.atDebug().log("BootstrapBuilder initialized");
    }

    @Override
    public IBoostrap withPackage(String packageName) {
        log.atTrace().log("Adding package: {}", packageName);
        Objects.requireNonNull(packageName, "Package name cannot be null");
        this.packages.add(packageName);

        // Propagate package to all IPackageableBuilder instances
        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IPackageableBuilder) {
                IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
                packageableBuilder.withPackage(packageName);
                log.atDebug().log("Package '{}' propagated to builder: {}",
                        packageName, builder.getClass().getSimpleName());
            }
        }

        log.atDebug().log("Package added: {}", packageName);
        return this;
    }

    @Override
    public IBoostrap withPackages(String[] packageNames) {
        log.atTrace().log("Adding {} packages", packageNames != null ? packageNames.length : 0);
        Objects.requireNonNull(packageNames, "Package names array cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        log.atDebug().log("All packages added");
        return this;
    }

    @Override
    public IBoostrap withBuilder(IBuilder<?> builder) {
        log.atTrace().log("Adding builder: {}", builder != null ? builder.getClass().getSimpleName() : "null");
        Objects.requireNonNull(builder, "Builder cannot be null");
        this.builders.add(builder);

        if (builder instanceof IPackageableBuilder && !this.packages.isEmpty()) {
            IPackageableBuilder<?, ?> packageableBuilder = (IPackageableBuilder<?, ?>) builder;
            for (String packageName : this.packages) {
                packageableBuilder.withPackage(packageName);
            }
            log.atDebug().log("Propagated {} packages to builder: {}",
                    this.packages.size(), builder.getClass().getSimpleName());
        }

        log.atInfo().log("Builder added: {}", builder.getClass().getSimpleName());
        return this;
    }

    @Override
    public String[] getPackages() {
        log.atTrace().log("Getting packages, count: {}", packages.size());
        return packages.toArray(new String[0]);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection()");

        this.packages.stream()
                .flatMap(packageName -> ObjectReflectionHelper.getClassesWithAnnotation(packageName, Bootstrap.class)
                        .stream())
                .forEach(builderClass -> {
                    try {
                        if (IBuilder.class.isAssignableFrom(builderClass)) {
                            IBuilder<?> builderInstance = (IBuilder<?>) ObjectReflectionHelper
                                    .instanciateNewObject(builderClass);
                            if (builderInstance instanceof IAutomaticBuilder) {
                                IAutomaticBuilder<?, ?> automaticBuilder = (IAutomaticBuilder<?, ?>) builderInstance;
                                automaticBuilder.autoDetect(true);
                                log.atInfo().log("Auto-detected builder {} with auto-detection enabled",
                                        builderClass.getSimpleName());
                            } else {
                                log.atInfo().log("Auto-detected builder {} (not automatic)",
                                        builderClass.getSimpleName());
                            }
                            this.withBuilder(builderInstance);
                        } else {
                            log.atWarn().log("Class {} has @Bootstrap annotation but does not implement IBuilder",
                                    builderClass.getName());
                        }
                    } catch (Exception e) {
                        throw new DslException("Failed to auto-detect builder: " + builderClass.getName(), e);
                    }
                });

        log.atInfo().log("Auto-detection completed for {} packages", packages.size());
        log.atTrace().log("Exiting doAutoDetection()");
    }

    @Override
    protected Object doBuild() throws DslException {
        log.atTrace().log("Entering doBuild()");

        if (this.builders.isEmpty()) {
            log.atWarn().log("No builders registered, returning null");
            return null;
        }

        log.atInfo().log("Building {} builders", this.builders.size());

        // Phase 1: Resolve dependencies between builders
        resolveDependencies();

        // Phase 2: Sort builders by dependency order (topological sort)
        List<IBuilder<?>> sortedBuilders = sortBuildersByDependencies();
        log.atInfo().log("Builders sorted by dependency order: {}",
                sortedBuilders.stream()
                        .map(b -> b.getClass().getSimpleName())
                        .toList());

        // Phase 3: Build all builders in dependency order
        List<Object> builtObjects = new ArrayList<>();
        for (IBuilder<?> builder : sortedBuilders) {
            log.atDebug().log("Building: {}", builder.getClass().getSimpleName());
            Object built = builder.build();
            builtObjects.add(built);
            log.atInfo().log("Successfully built: {}", builder.getClass().getSimpleName());
        }

        log.atInfo().log("Successfully built all {} builders", builtObjects.size());
        log.atTrace().log("Exiting doBuild()");

        return builtObjects.size() == 1 ? builtObjects.get(0) : builtObjects;
    }

    /**
     * Sorts builders by their dependencies using topological sort (Kahn's algorithm).
     * Builders with no dependencies are built first, then builders that depend on them, etc.
     *
     * @return list of builders sorted by dependency order
     * @throws DslException if there is a circular dependency
     */
    private List<IBuilder<?>> sortBuildersByDependencies() throws DslException {
        log.atTrace().log("Entering sortBuildersByDependencies()");

        Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph = new HashMap<>();
        Map<IBuilder<?>, Integer> inDegree = new HashMap<>();

        initializeDependencyGraph(dependencyGraph, inDegree);
        buildDependencyGraph(dependencyGraph, inDegree);
        List<IBuilder<?>> sortedBuilders = performTopologicalSort(dependencyGraph, inDegree);
        validateNoCyclicDependencies(sortedBuilders);

        log.atTrace().log("Exiting sortBuildersByDependencies()");
        return sortedBuilders;
    }

    /**
     * Initializes the dependency graph with all builders.
     */
    private void initializeDependencyGraph(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {
        for (IBuilder<?> builder : this.builders) {
            dependencyGraph.put(builder, new HashSet<>());
            inDegree.put(builder, 0);
        }
    }

    /**
     * Builds the dependency graph by analyzing builder dependencies.
     */
    private void buildDependencyGraph(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {
        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IDependentBuilder) {
                processDependentBuilder((IDependentBuilder<?, ?>) builder, dependencyGraph, inDegree);
            }
        }
    }

    /**
     * Processes a single dependent builder to update the dependency graph.
     */
    private void processDependentBuilder(
            IDependentBuilder<?, ?> dependentBuilder,
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {

        Set<Class<? extends IObservableBuilder<?, ?>>> allDeps = new HashSet<>();
        allDeps.addAll(dependentBuilder.require());
        allDeps.addAll(dependentBuilder.use());

        for (Class<? extends IObservableBuilder<?, ?>> depClass : allDeps) {
            IBuilder<?> dependency = findBuilderInstanceByClass(depClass);
            if (dependency != null) {
                dependencyGraph.get(dependency).add(dependentBuilder);
                inDegree.put(dependentBuilder, inDegree.get(dependentBuilder) + 1);
            }
        }
    }

    /**
     * Performs topological sort using Kahn's algorithm.
     */
    private List<IBuilder<?>> performTopologicalSort(
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree) {

        Queue<IBuilder<?>> queue = initializeQueueWithNoDependencies(inDegree);
        List<IBuilder<?>> sortedBuilders = new ArrayList<>();

        while (!queue.isEmpty()) {
            IBuilder<?> current = queue.poll();
            sortedBuilders.add(current);
            processBuilderDependents(current, dependencyGraph, inDegree, queue);
        }

        return sortedBuilders;
    }

    /**
     * Initializes the queue with builders that have no dependencies.
     */
    private Queue<IBuilder<?>> initializeQueueWithNoDependencies(Map<IBuilder<?>, Integer> inDegree) {
        Queue<IBuilder<?>> queue = new LinkedList<>();
        for (IBuilder<?> builder : this.builders) {
            if (inDegree.get(builder) == 0) {
                queue.add(builder);
                log.atDebug().log("Builder {} has no dependencies, will be built first",
                        builder.getClass().getSimpleName());
            }
        }
        return queue;
    }

    /**
     * Processes all builders that depend on the current builder.
     */
    private void processBuilderDependents(
            IBuilder<?> current,
            Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph,
            Map<IBuilder<?>, Integer> inDegree,
            Queue<IBuilder<?>> queue) {

        for (IBuilder<?> dependent : dependencyGraph.get(current)) {
            int newInDegree = inDegree.get(dependent) - 1;
            inDegree.put(dependent, newInDegree);
            if (newInDegree == 0) {
                queue.add(dependent);
                log.atDebug().log("Builder {} dependencies satisfied, adding to build queue",
                        dependent.getClass().getSimpleName());
            }
        }
    }

    /**
     * Validates that there are no cyclic dependencies.
     */
    private void validateNoCyclicDependencies(List<IBuilder<?>> sortedBuilders) throws DslException {
        if (sortedBuilders.size() != this.builders.size()) {
            List<String> notProcessed = this.builders.stream()
                    .filter(b -> !sortedBuilders.contains(b))
                    .map(b -> b.getClass().getSimpleName())
                    .toList();
            throw new DslException("Circular dependency detected among builders: " + notProcessed);
        }
    }

    /**
     * Finds a builder instance by its class.
     *
     * @param builderClass the class to search for
     * @return the builder instance or null if not found
     */
    private IBuilder<?> findBuilderInstanceByClass(Class<? extends IObservableBuilder<?, ?>> builderClass) {
        return this.builders.stream()
                .filter(b -> builderClass.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Resolves dependencies between builders by providing observable builders to
     * dependent builders.
     *
     * @throws DslException if dependency resolution fails
     */
    private void resolveDependencies() throws DslException {
        log.atTrace().log("Entering resolveDependencies()");

        List<IObservableBuilder<?, ?>> observableBuilders = collectObservableBuilders();
        log.atDebug().log("Found {} observable builders", observableBuilders.size());

        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IDependentBuilder) {
                resolveDependenciesForBuilder((IDependentBuilder<?, ?>) builder, observableBuilders);
            }
        }

        log.atTrace().log("Exiting resolveDependencies()");
    }

    /**
     * Collects all observable builders from the builders list.
     *
     * @return list of observable builders
     */
    private List<IObservableBuilder<?, ?>> collectObservableBuilders() {
        List<IObservableBuilder<?, ?>> result = new ArrayList<>();
        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IObservableBuilder) {
                result.add((IObservableBuilder<?, ?>) builder);
            }
        }
        return result;
    }

    /**
     * Resolves dependencies for a single dependent builder.
     *
     * @param dependentBuilder   the dependent builder
     * @param observableBuilders list of available observable builders
     * @throws DslException if a required dependency is not found
     */
    private void resolveDependenciesForBuilder(
            IDependentBuilder<?, ?> dependentBuilder,
            List<IObservableBuilder<?, ?>> observableBuilders) throws DslException {

        Set<Class<? extends IObservableBuilder<?, ?>>> requiredDeps = dependentBuilder.require();
        Set<Class<? extends IObservableBuilder<?, ?>>> usedDeps = dependentBuilder.use();

        log.atDebug().log("Builder {} requires {} dependencies and uses {} dependencies",
                dependentBuilder.getClass().getSimpleName(), requiredDeps.size(), usedDeps.size());

        provideRequiredDependencies(dependentBuilder, observableBuilders, requiredDeps);
        provideOptionalDependencies(dependentBuilder, observableBuilders, usedDeps);
    }

    /**
     * Provides required dependencies to a dependent builder.
     *
     * @param dependentBuilder   the dependent builder
     * @param observableBuilders list of available observable builders
     * @param requiredDeps       set of required dependency classes
     * @throws DslException if a required dependency is not found
     */
    private void provideRequiredDependencies(
            IDependentBuilder<?, ?> dependentBuilder,
            List<IObservableBuilder<?, ?>> observableBuilders,
            Set<Class<? extends IObservableBuilder<?, ?>>> requiredDeps) throws DslException {

        for (Class<? extends IObservableBuilder<?, ?>> depClass : requiredDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency == null) {
                throw new DslException("Required dependency not found: " + depClass.getName()
                        + " for builder: " + dependentBuilder.getClass().getSimpleName());
            }
            dependentBuilder.provide(dependency);
            log.atDebug().log("Provided required dependency {} to {}",
                    depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
        }
    }

    /**
     * Provides optional dependencies to a dependent builder.
     *
     * @param dependentBuilder   the dependent builder
     * @param observableBuilders list of available observable builders
     * @param usedDeps           set of optional dependency classes
     * @throws DslException if providing the dependency fails
     */
    private void provideOptionalDependencies(
            IDependentBuilder<?, ?> dependentBuilder,
            List<IObservableBuilder<?, ?>> observableBuilders,
            Set<Class<? extends IObservableBuilder<?, ?>>> usedDeps) throws DslException {

        for (Class<? extends IObservableBuilder<?, ?>> depClass : usedDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency != null) {
                dependentBuilder.provide(dependency);
                log.atDebug().log("Provided optional dependency {} to {}",
                        depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
            } else {
                log.atDebug().log("Optional dependency {} not available for {}",
                        depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
            }
        }
    }

    /**
     * Finds a builder by its class in the list of observable builders.
     *
     * @param builders     the list of observable builders
     * @param builderClass the class to search for
     * @return the builder instance or null if not found
     */
    private IObservableBuilder<?, ?> findBuilderByClass(
            List<IObservableBuilder<?, ?>> builders,
            Class<? extends IObservableBuilder<?, ?>> builderClass) {
        return builders.stream()
                .filter(b -> builderClass.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the list of registered builders.
     *
     * @return unmodifiable list of builders
     */
    protected List<IBuilder<?>> getBuilders() {
        return List.copyOf(builders);
    }

    /**
     * Gets the set of configured packages.
     *
     * @return unmodifiable set of packages
     */
    protected Set<String> getConfiguredPackages() {
        return Set.copyOf(packages);
    }

    @Override
    protected String[] getPackagesForScanning() {
        return packages.toArray(new String[0]);
    }

    @Override
    protected IAnnotationScanner getAnnotationScanner() {
        return ObjectReflectionHelper.getAnnotationScanner();
    }
}
