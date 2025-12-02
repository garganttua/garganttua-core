package com.garganttua.core.injection.context.validation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DependencyGraph {

    private final Map<Class<?>, Set<Class<?>>> adjacencyList = new LinkedHashMap<>();

    public void addDependency(Class<?> bean, Class<?> dependency) {
        log.atTrace().log("Entering addDependency(bean={}, dependency={})", bean, dependency);
        adjacencyList.computeIfAbsent(bean, k -> new LinkedHashSet<>()).add(dependency);
        log.atDebug().log("Added dependency {} to bean {}", dependency.getSimpleName(), bean.getSimpleName());
        log.atTrace().log("Exiting addDependency");
    }

    public Set<Class<?>> getDependencies(Class<?> bean) {
        log.atTrace().log("Entering getDependencies(bean={})", bean);
        Set<Class<?>> dependencies = adjacencyList.getOrDefault(bean, Set.of());
        log.atTrace().log("Exiting getDependencies with {} dependencies", dependencies.size());
        return dependencies;
    }

    public Set<Class<?>> getAllBeans() {
        log.atTrace().log("Entering getAllBeans()");
        Set<Class<?>> beans = adjacencyList.keySet();
        log.atTrace().log("Exiting getAllBeans with {} beans", beans.size());
        return beans;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DependencyGraph:\n");
        for (Map.Entry<Class<?>, Set<Class<?>>> entry : adjacencyList.entrySet()) {
            sb.append("Bean: ").append(entry.getKey().getSimpleName()).append(" -> Dependencies: ");
            if (entry.getValue().isEmpty()) {
                sb.append("[]");
            } else {
                sb.append("[");
                Iterator<Class<?>> it = entry.getValue().iterator();
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