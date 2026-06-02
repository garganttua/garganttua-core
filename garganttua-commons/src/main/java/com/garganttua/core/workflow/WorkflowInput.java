package com.garganttua.core.workflow;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable input passed to {@link IWorkflow#execute(WorkflowInput)}.
 *
 * <p>
 * Carries the main {@code payload} object plus a map of named {@code parameters}.
 * The parameters map is defensively wrapped as unmodifiable on construction, and a
 * {@code null} map is normalised to an empty one.
 * </p>
 *
 * @param payload    the primary input object for the workflow (may be {@code null})
 * @param parameters named parameters exposed to the workflow, never {@code null}
 * @since 2.0.0-ALPHA01
 * @see IWorkflow
 */
public record WorkflowInput(
    Object payload,
    Map<String, Object> parameters
) {
    public WorkflowInput {
        parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Collections.emptyMap();
    }

    /**
     * Creates an input with the given payload and no parameters.
     *
     * @param payload the primary input object (may be {@code null})
     * @return a new {@link WorkflowInput}
     */
    public static WorkflowInput of(Object payload) {
        return new WorkflowInput(payload, Collections.emptyMap());
    }

    /**
     * Creates an input with the given payload and parameters.
     *
     * @param payload the primary input object (may be {@code null})
     * @param params  the named parameters (a {@code null} map yields no parameters)
     * @return a new {@link WorkflowInput}
     */
    public static WorkflowInput of(Object payload, Map<String, Object> params) {
        return new WorkflowInput(payload, params);
    }

    /**
     * Creates an empty input with no payload and no parameters.
     *
     * @return a new empty {@link WorkflowInput}
     */
    public static WorkflowInput empty() {
        return new WorkflowInput(null, Collections.emptyMap());
    }
}
