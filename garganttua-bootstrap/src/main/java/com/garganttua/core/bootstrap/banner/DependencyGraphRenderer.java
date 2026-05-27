package com.garganttua.core.bootstrap.banner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.reflection.IClass;

/**
 * Renders an ASCII tree visualization of the Bootstrap dependency graph —
 * showing each builder, its declared deps and resolution direction in the
 * topological order Bootstrap will actually build.
 *
 * <pre>
 *   Dependency graph (topological build order):
 *     ReflectionBuilder
 *       ├─ InjectionContextBuilder   [requires ReflectionBuilder]
 *       │    ├─ ExpressionContextBuilder      [uses InjectionContextBuilder]
 *       │    └─ ObservabilityBuilder           [uses InjectionContextBuilder]
 *       └─ NativeConfigurationBuilder          [uses ReflectionBuilder]
 * </pre>
 *
 * <p>Mostly for cold-start debugging. The renderer is stateless and
 * tolerant of incomplete graphs (cycles flagged inline).
 *
 * @since 2.0.0-ALPHA02
 */
public final class DependencyGraphRenderer {

    private DependencyGraphRenderer() { /* static utility */ }

    /**
     * Render the dep graph as a sequence of lines (no trailing newlines).
     *
     * @param sortedBuilders builders in the topological order Bootstrap
     *                       will build them
     * @return one line per visible row, ready to print
     */
    public static List<String> render(List<IBuilder<?>> sortedBuilders) {
        List<String> out = new ArrayList<>();
        if (sortedBuilders == null || sortedBuilders.isEmpty()) {
            return out;
        }

        // Build an in-degree map (count of upstream classes each consumer
        // depends on) and a downstream-edge map (for each upstream class,
        // who consumes it).
        Map<String, List<IBuilder<?>>> downstream = new LinkedHashMap<>();
        Map<IBuilder<?>, List<String>> upstreamLabels = new LinkedHashMap<>();
        for (IBuilder<?> b : sortedBuilders) {
            upstreamLabels.put(b, new ArrayList<>());
            if (b instanceof IDependentBuilder<?, ?> dep) {
                for (IClass<? extends IObservableBuilder<?, ?>> r : dep.require()) {
                    upstreamLabels.get(b).add("requires " + r.getSimpleName());
                    downstream.computeIfAbsent(r.getSimpleName(), k -> new ArrayList<>()).add(b);
                }
                for (IClass<? extends IObservableBuilder<?, ?>> u : dep.use()) {
                    upstreamLabels.get(b).add("uses " + u.getSimpleName());
                    downstream.computeIfAbsent(u.getSimpleName(), k -> new ArrayList<>()).add(b);
                }
            }
        }

        out.add("Dependency graph (topological build order):");
        for (int i = 0; i < sortedBuilders.size(); i++) {
            IBuilder<?> b = sortedBuilders.get(i);
            String prefix = (i == sortedBuilders.size() - 1) ? "└─ " : "├─ ";
            List<String> labels = upstreamLabels.get(b);
            String depsTag = labels.isEmpty() ? "" : "   [" + String.join(", ", labels) + "]";
            out.add("  " + prefix + b.getClass().getSimpleName() + depsTag);
        }

        // Orphan check — flag classes referenced as deps but not in the
        // registry. Helps diagnose missing SPI providers etc.
        Set<String> registered = new java.util.HashSet<>();
        for (IBuilder<?> b : sortedBuilders) {
            registered.add(b.getClass().getSimpleName());
        }
        List<String> orphans = new ArrayList<>();
        for (String upstream : downstream.keySet()) {
            if (!registered.contains(upstream)) {
                orphans.add(upstream);
            }
        }
        if (!orphans.isEmpty()) {
            orphans.sort(Comparator.naturalOrder());
            out.add("  ! Unresolved upstream classes: " + String.join(", ", orphans));
        }

        return out;
    }
}
