package com.garganttua.core.injection.context.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.reflection.IClass;

/**
 * Detects circular dependencies in a {@link DependencyGraph} using a depth-first three-colour
 * traversal, failing fast with a formatted cycle description when one is found.
 */
public class DependencyCycleDetector {
    private static final Logger log = Logger.getLogger(DependencyCycleDetector.class);

    private enum VisitState {
        UNVISITED, VISITING, VISITED
    }

    /**
     * Runs cycle detection over the whole graph.
     *
     * @param graph the dependency graph to inspect
     * @throws DiException if a circular dependency is detected, with the offending chain in its message
     */
    public void detectCycles(DependencyGraph graph) throws DiException {
        log.trace("Entering detectCycles with graph: {}", graph);

        Map<IClass<?>, VisitState> state = new LinkedHashMap<>();

        for (IClass<?> bean : graph.getAllBeans()) {
            state.put(bean, VisitState.UNVISITED);
            log.debug("Marking bean {} as UNVISITED", bean.getSimpleName());
        }

        for (IClass<?> bean : graph.getAllBeans()) {
            for (IClass<?> dep : graph.getDependencies(bean)) {
                state.putIfAbsent(dep, VisitState.UNVISITED);
                log.debug("Ensuring dependency {} is tracked", dep.getSimpleName());
            }
        }

        for (IClass<?> bean : state.keySet()) {
            if (state.get(bean) == VisitState.UNVISITED) {
                log.debug("Starting DFS for bean {}", bean.getSimpleName());
                dfs(graph, bean, state, new ArrayDeque<>());
            } else {
                log.trace("Skipping bean {} as it is already visited", bean.getSimpleName());
            }
        }

        log.trace("Exiting detectCycles");
    }

    private void dfs(DependencyGraph graph, IClass<?> current,
                     Map<IClass<?>, VisitState> state, Deque<IClass<?>> stack) throws DiException {

        log.trace("Entering dfs with current bean: {}", current.getSimpleName());
        state.put(current, VisitState.VISITING);
        stack.push(current);
        log.debug("Marking {} as VISITING and pushing to stack", current.getSimpleName());

        for (IClass<?> dep : graph.getDependencies(current)) {
            VisitState depState = state.get(dep);

            if (depState == null) {
                state.put(dep, VisitState.UNVISITED);
                depState = VisitState.UNVISITED;
                log.warn("Dependency {} was missing in state map, initializing as UNVISITED", dep.getSimpleName());
            }

            if (depState == VisitState.VISITING) {
                String cycle = formatCycle(stack, dep);
                log.error("Circular dependency detected: {}", cycle);
                throw new DiException("Circular dependency detected: " + cycle);
            } else if (depState == VisitState.UNVISITED) {
                log.debug("Recursing into dependency {} from bean {}", dep.getSimpleName(), current.getSimpleName());
                dfs(graph, dep, state, stack);
            } else {
                log.trace("Dependency {} already visited", dep.getSimpleName());
            }
        }

        stack.pop();
        state.put(current, VisitState.VISITED);
        log.debug("Finished DFS for {}, marking as VISITED and popping from stack", current.getSimpleName());
        log.trace("Exiting dfs for bean: {}", current.getSimpleName());
    }

    private String formatCycle(Deque<IClass<?>> stack, IClass<?> start) {
        log.trace("Entering formatCycle for start bean: {}", start.getSimpleName());
        List<IClass<?>> path = new ArrayList<>();
        Iterator<IClass<?>> descIt = stack.descendingIterator();
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

        StringBuilder sb = new StringBuilder();
        if (idx == -1) {
            log.warn("Start bean {} not found in stack path, using fallback format", start.getSimpleName());
            for (IClass<?> c : path) {
                sb.append(c.getSimpleName()).append(" -> ");
            }
            sb.append(start.getSimpleName());
        } else {
            for (int i = idx; i < path.size(); i++) {
                sb.append(path.get(i).getSimpleName()).append(" -> ");
            }
            sb.append(start.getSimpleName());
        }

        String cycleStr = sb.toString();
        log.debug("Formatted cycle: {}", cycleStr);
        log.trace("Exiting formatCycle for start bean: {}", start.getSimpleName());
        return cycleStr;
    }
}