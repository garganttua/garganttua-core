package com.garganttua.core.workflow;

import java.util.Collections;
import java.util.Map;

public record WorkflowInput(
    Object payload,
    Map<String, Object> parameters
) {
    public WorkflowInput {
        parameters = parameters != null ? Collections.unmodifiableMap(parameters) : Collections.emptyMap();
    }

    public static WorkflowInput of(Object payload) {
        return new WorkflowInput(payload, Collections.emptyMap());
    }

    public static WorkflowInput of(Object payload, Map<String, Object> params) {
        return new WorkflowInput(payload, params);
    }

    public static WorkflowInput empty() {
        return new WorkflowInput(null, Collections.emptyMap());
    }
}
