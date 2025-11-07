package com.garganttua.core.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RuntimeProcess {

    @Getter
    private final Map<String, Map<String, String>> stages;

    public RuntimeProcess(Map<String, Map<String, String>> stages) {
        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        stages.forEach((stage, stepMap) -> copy.put(stage, Collections.unmodifiableMap(new LinkedHashMap<>(stepMap))));
        this.stages = Collections.unmodifiableMap(copy);
    }

    public void print() {
        System.out.println("==== Runtime Process ====");
        stages.forEach((stage, stepMap) -> {
            System.out.println("Stage: " + stage);
            stepMap.forEach((step, variable) -> System.out.println("  Step: " + step + " -> Variable: " + variable));
        });
        System.out.println("=========================");
    }

}
