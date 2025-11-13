package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.garganttua.core.reflection.binders.IMethodBinder;

public class RuntimeStage implements IRuntimeStage {

    private final Map<String, IMethodBinder<?>> steps = new LinkedHashMap<>();
    private final String stageName;

    public RuntimeStage(String stageName, Map<String, IMethodBinder<?>> steps) {
        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
        if (steps != null) {
            this.steps.putAll(steps);
        }
    }

    @Override
    public String getStageName() {
        return stageName;
    }

    @Override
    public IMethodBinder<?> getStep(String stepName) {
        return steps.get(stepName);
    }

    @Override
    public Map<String, IMethodBinder<?>> getSteps() {
        return Collections.unmodifiableMap(steps);
    }

}
