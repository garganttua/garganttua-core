package com.garganttua.core.runtime.dsl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.garganttua.core.runtime.RuntimeProcess;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeProcessBuilder {

    private final Map<String, Map<String, String>> process = new LinkedHashMap<>();

    public RuntimeProcessBuilder addStep(String stageName, String stepName, String variableName) {
        if (stageName == null || stepName == null || variableName == null) {
            log.warn("Attempt to add step with null parameter(s): stage={}, step={}, variable={}",
                    stageName, stepName, variableName);
            return this;
        }

        String stage = stageName.trim();
        String step = stepName.trim();
        String variable = variableName.trim();

        try {
            Map<String, String> steps = process.computeIfAbsent(stage, k -> new LinkedHashMap<>());

            if (steps.containsKey(step)) {
                String existingVar = steps.get(step);
                if (existingVar.equals(variable)) {
                    log.debug("Step [{} -> {} -> {}] already exists — skipping duplicate", stage, step, variable);
                    return this;
                } else {
                    log.warn("Step [{} -> {}] already exists with a different variable: {} (new={})",
                            stage, step, existingVar, variable);
                    return this;
                }
            }

            steps.put(step, variable);

            if (log.isTraceEnabled()) {
                log.trace("Step added → Stage: {}, Step: {}, Variable: {}", stage, step, variable);
            }

            if (log.isDebugEnabled()) {
                log.debug("Added new step [{} -> {} -> {}] to runtime process", stage, step, variable);
            }

            log.info("Runtime step registered: {}.{} mapped to {}", stage, step, variable);

        } catch (Exception e) {
            log.error("Unexpected error while adding step [stage={}, step={}, variable={}]",
                    stage, step, variable, e);
            throw e;
        }

        return this;
    }

    public RuntimeProcess build() {
        log.info("Building RuntimeProcess with {} stages", process.size());

        if (log.isDebugEnabled()) {
            process.forEach((stage, stepMap) -> {
                log.debug("Stage [{}] with {} steps", stage, stepMap.size());
                stepMap.forEach((step, variable) -> log.trace("  Step [{}] → Variable [{}]", step, variable));
            });
        }

        log.info("RuntimeProcess successfully built.");
        return new RuntimeProcess(process);
    }

    public void print() {
        log.info("==== Runtime Process ====");
        process.forEach((stage, stepMap) -> {
            log.info("Stage: {}", stage);
            stepMap.forEach((step, variable) -> log.info("  Step: {} -> Variable: {}", step, variable));
        });
        log.info("=========================");
    }
}