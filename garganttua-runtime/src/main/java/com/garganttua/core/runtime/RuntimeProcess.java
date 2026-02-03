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
        log.atTrace().log("[RuntimeProcess.<init>] Initializing RuntimeProcess with stages={}", stages);

        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        stages.forEach((stage, stepMap) -> copy.put(stage, Collections.unmodifiableMap(new LinkedHashMap<>(stepMap))));
        this.stages = Collections.unmodifiableMap(copy);

        log.atDebug().log("[RuntimeProcess.<init>] RuntimeProcess initialized with {} stages", this.stages.size());
    }

    public void print() {
        log.atDebug().log("[RuntimeProcess.print] Printing runtime process");

        System.out.println("==== Runtime Process ====");
        stages.forEach((stage, stepMap) -> {
            log.atDebug().log("[RuntimeProcess.print] Stage: {} with {} steps", stage, stepMap.size());
            System.out.println("Stage: " + stage);
            stepMap.forEach((step, variable) -> {
                log.atTrace().log("[RuntimeProcess.print] Step: {} -> Variable: {}", step, variable);
                System.out.println("  Step: " + step + " -> Variable: " + variable);
            });
        });
        System.out.println("=========================");

        log.atDebug().log("[RuntimeProcess.print] Finished printing runtime process");
    }

}
