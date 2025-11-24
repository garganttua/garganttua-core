package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeStage<InputType, OutputType> implements IRuntimeStage<InputType, OutputType> {

    private final Map<String, IRuntimeStep<?, InputType, OutputType>> steps = new LinkedHashMap<>();
    private final String stageName;

    public RuntimeStage(String stageName, Map<String, IRuntimeStep<?, InputType, OutputType>> steps) {
        log.atTrace().log("[RuntimeStage.<init>] Initializing RuntimeStage with name={} and steps={}", stageName,
                steps);

        this.stageName = Objects.requireNonNull(stageName, "Stage name cannot be null");
        if (steps != null) {
            this.steps.putAll(steps);
        }

        log.atInfo().log("[RuntimeStage.<init>] RuntimeStage '{}' initialized with {} steps", this.stageName,
                this.steps.size());
    }

    @Override
    public String getStageName() {
        log.atTrace().log("[RuntimeStage.getStageName] Returning stage name: {}", stageName);
        return stageName;
    }

    @Override
    public IRuntimeStep<?, InputType, OutputType> getStep(String stepName) {
        log.atDebug().log("[RuntimeStage.getStep] Fetching step '{}' in stage '{}'", stepName, stageName);
        return steps.get(stepName);
    }

    @Override
    public Map<String, IRuntimeStep<?, InputType, OutputType>> getSteps() {
        log.atTrace().log("[RuntimeStage.getSteps] Returning {} steps for stage '{}'", steps.size(), stageName);
        return Collections.unmodifiableMap(steps);
    }

}
