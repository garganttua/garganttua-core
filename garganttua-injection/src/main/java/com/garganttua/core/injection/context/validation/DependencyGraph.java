package com.garganttua.core.injection.context.validation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class DependencyGraph {

    private final Map<Class<?>, Set<Class<?>>> adjacencyList = new LinkedHashMap<>();

    public void addDependency(Class<?> bean, Class<?> dependency) {
        adjacencyList.computeIfAbsent(bean, k -> new LinkedHashSet<>()).add(dependency);
    }

    public Set<Class<?>> getDependencies(Class<?> bean) {
        return adjacencyList.getOrDefault(bean, Set.of());
    }

    public Set<Class<?>> getAllBeans() {
        return adjacencyList.keySet();
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