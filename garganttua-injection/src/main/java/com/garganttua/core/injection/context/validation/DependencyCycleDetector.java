package com.garganttua.core.injection.context.validation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.garganttua.core.injection.DiException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DependencyCycleDetector {

    private enum VisitState {
        UNVISITED, VISITING, VISITED
    }

    public void detectCycles(DependencyGraph graph) throws DiException {
        log.atTrace().log("Entering detectCycles with graph: {}", graph);

        Map<Class<?>, VisitState> state = new LinkedHashMap<>();

        for (Class<?> bean : graph.getAllBeans()) {
            state.put(bean, VisitState.UNVISITED);
            log.atDebug().log("Marking bean {} as UNVISITED", bean.getSimpleName());
        }

        for (Class<?> bean : graph.getAllBeans()) {
            for (Class<?> dep : graph.getDependencies(bean)) {
                state.putIfAbsent(dep, VisitState.UNVISITED);
                log.atDebug().log("Ensuring dependency {} is tracked", dep.getSimpleName());
            }
        }

        for (Class<?> bean : state.keySet()) {
            if (state.get(bean) == VisitState.UNVISITED) {
                log.atInfo().log("Starting DFS for bean {}", bean.getSimpleName());
                dfs(graph, bean, state, new ArrayDeque<>());
            } else {
                log.atTrace().log("Skipping bean {} as it is already visited", bean.getSimpleName());
            }
        }

        log.atTrace().log("Exiting detectCycles");
    }

    private void dfs(DependencyGraph graph, Class<?> current,
                     Map<Class<?>, VisitState> state, Deque<Class<?>> stack) throws DiException {

        log.atTrace().log("Entering dfs with current bean: {}", current.getSimpleName());
        state.put(current, VisitState.VISITING);
        stack.push(current);
        log.atDebug().log("Marking {} as VISITING and pushing to stack", current.getSimpleName());

        for (Class<?> dep : graph.getDependencies(current)) {
            VisitState depState = state.get(dep);

            if (depState == null) {
                state.put(dep, VisitState.UNVISITED);
                depState = VisitState.UNVISITED;
                log.atWarn().log("Dependency {} was missing in state map, initializing as UNVISITED", dep.getSimpleName());
            }

            if (depState == VisitState.VISITING) {
                String cycle = formatCycle(stack, dep);
                log.atError().log("Circular dependency detected: {}", cycle);
                throw new DiException("Circular dependency detected: " + cycle);
            } else if (depState == VisitState.UNVISITED) {
                log.atInfo().log("Recursing into dependency {} from bean {}", dep.getSimpleName(), current.getSimpleName());
                dfs(graph, dep, state, stack);
            } else {
                log.atTrace().log("Dependency {} already visited", dep.getSimpleName());
            }
        }

        stack.pop();
        state.put(current, VisitState.VISITED);
        log.atDebug().log("Finished DFS for {}, marking as VISITED and popping from stack", current.getSimpleName());
        log.atTrace().log("Exiting dfs for bean: {}", current.getSimpleName());
    }

    private String formatCycle(Deque<Class<?>> stack, Class<?> start) {
        log.atTrace().log("Entering formatCycle for start bean: {}", start.getSimpleName());
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

        StringBuilder sb = new StringBuilder();
        if (idx == -1) {
            log.atWarn().log("Start bean {} not found in stack path, using fallback format", start.getSimpleName());
            for (Class<?> c : path) {
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
        log.atDebug().log("Formatted cycle: {}", cycleStr);
        log.atTrace().log("Exiting formatCycle for start bean: {}", start.getSimpleName());
        return cycleStr;
    }
}