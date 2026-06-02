package com.garganttua.core.bootstrap.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Builder dependency resolution for {@link Bootstrap}: topological sort of the
 * registered builders (Kahn's algorithm, with cycle detection) and wiring of
 * required/optional dependencies onto each {@link IDependentBuilder}. Operates on
 * a snapshot of the builders taken at construction. Extracted to keep
 * {@code Bootstrap} focused on orchestration.
 */
final class BuilderDependencyResolver {

    private static final Logger log = Logger.getLogger(BuilderDependencyResolver.class);

    private final List<IBuilder<?>> builders;
    private final List<IObservableBuilder<?, ?>> providedBuilders;

    BuilderDependencyResolver(List<IBuilder<?>> builders, List<IObservableBuilder<?, ?>> providedBuilders) {
        this.builders = builders;
        this.providedBuilders = providedBuilders;
    }

    List<IBuilder<?>> sortBuildersByDependencies() throws DslException {
        log.trace("Entering sortBuildersByDependencies()");

        Map<IBuilder<?>, Set<IBuilder<?>>> dependencyGraph = new HashMap<>();
        Map<IBuilder<?>, Integer> inDegree = new HashMap<>();

        initializeDependencyGraph(dependencyGraph, inDegree);
        buildDependencyGraph(dependencyGraph, inDegree);
        List<IBuilder<?>> sortedBuilders = performTopologicalSort(dependencyGraph, inDegree);
        validateNoCyclicDependencies(sortedBuilders);

        log.trace("Exiting sortBuildersByDependencies()");
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

        Set<IClass<? extends IObservableBuilder<?, ?>>> allDeps = new HashSet<>();
        allDeps.addAll(dependentBuilder.require());
        allDeps.addAll(dependentBuilder.use());

        for (IClass<? extends IObservableBuilder<?, ?>> depClass : allDeps) {
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
                log.debug("Builder {} has no dependencies, will be built first",
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
                log.debug("Builder {} dependencies satisfied, adding to build queue",
                        dependent.getClass().getSimpleName());
            }
        }
    }

    /**
     * Validates that there are no cyclic dependencies.
     */
    private void validateNoCyclicDependencies(List<IBuilder<?>> sortedBuilders) throws DslException {
        List<IBuilder<?>> allBuilders = this.builders;
        if (sortedBuilders.size() != allBuilders.size()) {
            List<String> notProcessed = allBuilders.stream()
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
    private IBuilder<?> findBuilderInstanceByClass(IClass<? extends IObservableBuilder<?, ?>> builderClass) {
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
    void resolveDependencies() throws DslException {
        log.trace("Entering resolveDependencies()");

        List<IObservableBuilder<?, ?>> observableBuilders = collectObservableBuilders();
        log.debug("Found {} observable builders", observableBuilders.size());

        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IDependentBuilder) {
                resolveDependenciesForBuilder((IDependentBuilder<?, ?>) builder, observableBuilders);
            }
        }

        log.trace("Exiting resolveDependencies()");
    }

    /**
     * Collects all observable builders that can serve as dependencies for
     * other builders' dep resolution. Includes both managed builders (registered
     * via {@code withBuilder()}, which Phase 3 lifecycle-manages) AND provided
     * deps (registered via {@code provide()}, which are NOT lifecycle-managed
     * here — their consumer owns their lifecycle).
     *
     * @return list of observable builders visible to dependency resolution
     */
    private List<IObservableBuilder<?, ?>> collectObservableBuilders() {
        List<IObservableBuilder<?, ?>> result = new ArrayList<>();
        for (IBuilder<?> builder : this.builders) {
            if (builder instanceof IObservableBuilder) {
                result.add((IObservableBuilder<?, ?>) builder);
            }
        }
        // Provided deps are also candidates for satisfying other builders'
        // require() / use() — e.g. SPI-loaded InjectionContextBuilder that
        // we register via provide() (not withBuilder()) to avoid Phase 3
        // pre-initializing it.
        for (IObservableBuilder<?, ?> provided : this.providedBuilders) {
            if (!result.contains(provided)) {
                result.add(provided);
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

        Set<IClass<? extends IObservableBuilder<?, ?>>> requiredDeps = dependentBuilder.require();
        Set<IClass<? extends IObservableBuilder<?, ?>>> usedDeps = dependentBuilder.use();

        log.debug("Builder {} requires {} dependencies and uses {} dependencies",
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
            Set<IClass<? extends IObservableBuilder<?, ?>>> requiredDeps) throws DslException {

        for (IClass<? extends IObservableBuilder<?, ?>> depClass : requiredDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency == null) {
                throw new DslException("Required dependency not found: " + depClass.getName()
                        + " for builder: " + dependentBuilder.getClass().getSimpleName());
            }
            dependentBuilder.provide(dependency);
            log.debug("Provided required dependency {} to {}",
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
            Set<IClass<? extends IObservableBuilder<?, ?>>> usedDeps) throws DslException {

        for (IClass<? extends IObservableBuilder<?, ?>> depClass : usedDeps) {
            IObservableBuilder<?, ?> dependency = findBuilderByClass(observableBuilders, depClass);
            if (dependency != null) {
                dependentBuilder.provide(dependency);
                log.debug("Provided optional dependency {} to {}",
                        depClass.getSimpleName(), dependentBuilder.getClass().getSimpleName());
            } else {
                log.debug("Optional dependency {} not available for {}",
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
            IClass<? extends IObservableBuilder<?, ?>> builderClass) {
        return builders.stream()
                .filter(b -> builderClass.isAssignableFrom(b.getClass()))
                .findFirst()
                .orElse(null);
    }
}
