package com.garganttua.core.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;

/**
 * Registry holding the workflows produced by {@code WorkflowsBuilder} and
 * exposing summary information to Bootstrap via
 * {@link IBootstrapSummaryContributor}.
 *
 * <p>Mirrors {@code RuntimesRegistry} — wraps {@code Map<String, IWorkflow>}
 * for backward compatibility while surfacing useful counters in the
 * Bootstrap startup banner.
 *
 * @since 2.0.0-ALPHA02
 */
public class WorkflowsRegistry implements IBootstrapSummaryContributor, Map<String, IWorkflow> {

    private final Map<String, IWorkflow> workflows;

    /**
     * Creates an immutable registry from the given name-to-workflow map.
     *
     * @param workflows the workflows to register, keyed by name; defensively copied
     * @throws NullPointerException if {@code workflows} is {@code null}
     */
    public WorkflowsRegistry(Map<String, IWorkflow> workflows) {
        Objects.requireNonNull(workflows, "Workflows map cannot be null");
        this.workflows = Collections.unmodifiableMap(new LinkedHashMap<>(workflows));
    }

    /**
     * Looks up a registered workflow by name.
     *
     * @param name the workflow name
     * @return the workflow, or empty if none is registered under {@code name}
     */
    public Optional<IWorkflow> getWorkflow(String name) {
        return Optional.ofNullable(this.workflows.get(name));
    }

    /** {@return an unmodifiable view of all registered workflows keyed by name} */
    public Map<String, IWorkflow> getAll() {
        return this.workflows;
    }

    // --- IBootstrapSummaryContributor ---

    @Override
    public String getSummaryCategory() {
        return "Workflow Engine";
    }

    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Workflows registered", String.valueOf(this.workflows.size()));
        long precompiled = this.workflows.values().stream()
                .filter(IWorkflow::isPrecompiled)
                .count();
        items.put("Precompiled workflows", String.valueOf(precompiled));
        if (!this.workflows.isEmpty()) {
            String names = String.join(", ", this.workflows.keySet());
            if (names.length() > 50) {
                names = names.substring(0, 47) + "...";
            }
            items.put("Workflow names", names);
        }
        return items;
    }

    // --- Map delegation ---

    @Override public int size() { return this.workflows.size(); }
    @Override public boolean isEmpty() { return this.workflows.isEmpty(); }
    @Override public boolean containsKey(Object key) { return this.workflows.containsKey(key); }
    @Override public boolean containsValue(Object value) { return this.workflows.containsValue(value); }
    @Override public IWorkflow get(Object key) { return this.workflows.get(key); }
    @Override public IWorkflow put(String key, IWorkflow value) {
        throw new UnsupportedOperationException("WorkflowsRegistry is immutable");
    }
    @Override public IWorkflow remove(Object key) {
        throw new UnsupportedOperationException("WorkflowsRegistry is immutable");
    }
    @Override public void putAll(Map<? extends String, ? extends IWorkflow> m) {
        throw new UnsupportedOperationException("WorkflowsRegistry is immutable");
    }
    @Override public void clear() {
        throw new UnsupportedOperationException("WorkflowsRegistry is immutable");
    }
    @Override public Set<String> keySet() { return this.workflows.keySet(); }
    @Override public Collection<IWorkflow> values() { return this.workflows.values(); }
    @Override public Set<Entry<String, IWorkflow>> entrySet() { return this.workflows.entrySet(); }
}
