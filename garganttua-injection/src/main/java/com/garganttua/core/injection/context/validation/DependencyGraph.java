package com.garganttua.core.injection.context.validation;

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
}