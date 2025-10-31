package com.garganttua.injection.beans;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DependencyGraph {

    private final Map<Class<?>, Set<Class<?>>> adjacencyList = new HashMap<>();

    public void addDependency(Class<?> bean, Class<?> dependency) {
        adjacencyList.computeIfAbsent(bean, k -> new HashSet<>()).add(dependency);
    }

    public Set<Class<?>> getDependencies(Class<?> bean) {
        return adjacencyList.getOrDefault(bean, Set.of());
    }

    public Set<Class<?>> getAllBeans() {
        return adjacencyList.keySet();
    }
}