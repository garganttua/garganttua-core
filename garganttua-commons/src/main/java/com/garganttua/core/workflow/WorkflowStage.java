package com.garganttua.core.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable definition of a workflow stage: an ordered group of
 * {@link WorkflowScript scripts} sharing optional wrap, catch and condition
 * expressions.
 *
 * @param name                      the stage name
 * @param scripts                   the ordered scripts (stored unmodifiable)
 * @param wrapExpression            an expression wrapping the whole stage, or {@code null}
 * @param catchExpression           the stage-level immediate catch expression, or {@code null}
 * @param catchDownstreamExpression the stage-level downstream catch expression, or {@code null}
 * @param condition                 the stage {@code when} condition, or {@code null}
 */
public record WorkflowStage(
    String name,
    List<WorkflowScript> scripts,
    String wrapExpression,
    String catchExpression,
    String catchDownstreamExpression,
    String condition
) {
    public WorkflowStage {
        scripts = scripts != null ? Collections.unmodifiableList(new ArrayList<>(scripts)) : Collections.emptyList();
    }

    /**
     * @return {@code true} if this stage declares a non-empty wrapper expression
     */
    public boolean hasWrap() {
        return wrapExpression != null && !wrapExpression.isEmpty();
    }

    /**
     * @return {@code true} if this stage declares an immediate or downstream catch clause
     */
    public boolean hasCatch() {
        return (catchExpression != null && !catchExpression.isEmpty())
            || (catchDownstreamExpression != null && !catchDownstreamExpression.isEmpty());
    }

    /**
     * Creates a stage with the given scripts and no wrap/catch/condition.
     *
     * @return a new {@code WorkflowStage}
     */
    public static WorkflowStage of(String name, List<WorkflowScript> scripts) {
        return new WorkflowStage(name, scripts, null, null, null, null);
    }

    /**
     * Creates a stage from a varargs list of scripts and no wrap/catch/condition.
     *
     * @return a new {@code WorkflowStage}
     */
    public static WorkflowStage of(String name, WorkflowScript... scripts) {
        return new WorkflowStage(name, List.of(scripts), null, null, null, null);
    }
}
