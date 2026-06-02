package com.garganttua.core.injection.context.validation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;

/**
 * Directed graph of bean-to-dependency edges, backed by an insertion-ordered adjacency list,
 * used as input to {@link DependencyCycleDetector}.
 */
public class DependencyGraph {
    private static final Logger log = Logger.getLogger(DependencyGraph.class);

    private final Map<IClass<?>, Set<IClass<?>>> adjacencyList = new LinkedHashMap<>();

    /**
     * Records that {@code bean} depends on {@code dependency}.
     *
     * @param bean the dependent bean type
     * @param dependency the type {@code bean} depends on
     */
    public void addDependency(IClass<?> bean, IClass<?> dependency) {
        log.trace("Entering addDependency(bean={}, dependency={})", bean, dependency);
        adjacencyList.computeIfAbsent(bean, k -> new LinkedHashSet<>()).add(dependency);
        log.debug("Added dependency {} to bean {}", dependency.getSimpleName(), bean.getSimpleName());
        log.trace("Exiting addDependency");
    }

    /**
     * Returns the direct dependencies declared for a bean.
     *
     * @param bean the bean type to look up
     * @return the dependency set, or an empty set if the bean has no recorded edges
     */
    public Set<IClass<?>> getDependencies(IClass<?> bean) {
        log.trace("Entering getDependencies(bean={})", bean);
        Set<IClass<?>> dependencies = adjacencyList.getOrDefault(bean, Set.of());
        log.trace("Exiting getDependencies with {} dependencies", dependencies.size());
        return dependencies;
    }

    /**
     * Returns every bean that has at least one outgoing dependency edge.
     *
     * @return the set of beans known to the graph (those used as edge sources)
     */
    public Set<IClass<?>> getAllBeans() {
        log.trace("Entering getAllBeans()");
        Set<IClass<?>> beans = adjacencyList.keySet();
        log.trace("Exiting getAllBeans with {} beans", beans.size());
        return beans;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DependencyGraph:\n");
        for (Map.Entry<IClass<?>, Set<IClass<?>>> entry : adjacencyList.entrySet()) {
            sb.append("Bean: ").append(entry.getKey().getSimpleName()).append(" -> Dependencies: ");
            if (entry.getValue().isEmpty()) {
                sb.append("[]");
            } else {
                sb.append("[");
                Iterator<IClass<?>> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    sb.append(it.next().getSimpleName());
                    if (it.hasNext())
                        sb.append(", ");
                }
                sb.append("]");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}