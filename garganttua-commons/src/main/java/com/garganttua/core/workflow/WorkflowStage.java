package com.garganttua.core.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record WorkflowStage(
    String name,
    List<WorkflowScript> scripts,
    String wrapExpression,
    String catchExpression,
    String catchDownstreamExpression
) {
    public WorkflowStage {
        scripts = scripts != null ? Collections.unmodifiableList(new ArrayList<>(scripts)) : Collections.emptyList();
    }

    /**
     * Checks if this stage has a wrapper expression.
     */
    public boolean hasWrap() {
        return wrapExpression != null && !wrapExpression.isEmpty();
    }

    /**
     * Checks if this stage has any catch clauses.
     */
    public boolean hasCatch() {
        return (catchExpression != null && !catchExpression.isEmpty())
            || (catchDownstreamExpression != null && !catchDownstreamExpression.isEmpty());
    }

    public static WorkflowStage of(String name, List<WorkflowScript> scripts) {
        return new WorkflowStage(name, scripts, null, null, null);
    }

    public static WorkflowStage of(String name, WorkflowScript... scripts) {
        return new WorkflowStage(name, List.of(scripts), null, null, null);
    }
}
