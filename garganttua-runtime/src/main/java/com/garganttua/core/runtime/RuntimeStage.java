package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class RuntimeStage<InputType, OutputType> implements IRuntimeStage<InputType, OutputType> {

    private final Map<String, IRuntimeStep<?,InputType, OutputType>> steps = new LinkedHashMap<>();
    private final String stageName;

    public RuntimeStage(String stageName, Map<String, IRuntimeStep<?,InputType, OutputType>> steps) {
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
    public IRuntimeStep<?,InputType, OutputType> getStep(String stepName) {
        return steps.get(stepName);
    }

    @Override
    public Map<String, IRuntimeStep<?,InputType, OutputType>> getSteps() {
        return Collections.unmodifiableMap(steps);
    }

}
