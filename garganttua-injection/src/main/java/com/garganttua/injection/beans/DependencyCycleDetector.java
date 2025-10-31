package com.garganttua.injection.beans;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.injection.DiException;

public class DependencyCycleDetector {

    private enum VisitState {
        UNVISITED, VISITING, VISITED
    }

    public void detectCycles(DependencyGraph graph) throws DiException {
        Map<Class<?>, VisitState> state = new LinkedHashMap<>();

        for (Class<?> bean : graph.getAllBeans()) {
            state.put(bean, VisitState.UNVISITED);
        }
        for (Class<?> bean : graph.getAllBeans()) {
            for (Class<?> dep : graph.getDependencies(bean)) {
                state.putIfAbsent(dep, VisitState.UNVISITED);
            }
        }

        for (Class<?> bean : state.keySet()) {
            if (state.get(bean) == VisitState.UNVISITED) {
                dfs(graph, bean, state, new ArrayDeque<>());
            }
        }
    }

    private void dfs(DependencyGraph graph, Class<?> current,
            Map<Class<?>, VisitState> state, Deque<Class<?>> stack) throws DiException {

        state.put(current, VisitState.VISITING);
        stack.push(current);

        for (Class<?> dep : graph.getDependencies(current)) {
            VisitState depState = state.get(dep);

            if (depState == null) {
                state.put(dep, VisitState.UNVISITED);
                depState = VisitState.UNVISITED;
            }

            if (depState == VisitState.VISITING) {
                throw new DiException("Circular dependency detected: " + formatCycle(stack, dep));
            } else if (depState == VisitState.UNVISITED) {
                dfs(graph, dep, state, stack);
            }
        }

        stack.pop();
        state.put(current, VisitState.VISITED);
    }

    /**
     * Reconstruit la séquence du cycle à partir de la pile (stack).
     * On retourne une chaîne lisible du type "A -> B -> C -> A".
     */
    private String formatCycle(Deque<Class<?>> stack, Class<?> start) {
        List<Class<?>> path = new ArrayList<>();
        Iterator<Class<?>> descIt = stack.descendingIterator();
        while (descIt.hasNext()) {
            path.add(descIt.next());
        }

        int idx = -1;
        for (int i = 0; i < path.size(); i++) {
            if (path.get(i).equals(start)) {
                idx = i;
                break;
            }
        }

        if (idx == -1) {
            StringBuilder fallback = new StringBuilder();
            for (Class<?> c : path) {
                fallback.append(c.getSimpleName()).append(" -> ");
            }
            fallback.append(start.getSimpleName());
            return fallback.toString();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < path.size(); i++) {
            sb.append(path.get(i).getSimpleName()).append(" -> ");
        }

        sb.append(start.getSimpleName());
        return sb.toString();
    }
}
