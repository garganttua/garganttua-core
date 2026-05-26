package com.garganttua.core.runtime;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

public class RuntimeProcess {
    private static final IDiagnostic log = Diagnostics.of(RuntimeProcess.class);

    @Getter
    private final Map<String, Map<String, String>> stages;

    public RuntimeProcess(Map<String, Map<String, String>> stages) {
        log.trace("[RuntimeProcess.<init>] Initializing RuntimeProcess with stages={}", stages);

        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        stages.forEach((stage, stepMap) -> copy.put(stage, Collections.unmodifiableMap(new LinkedHashMap<>(stepMap))));
        this.stages = Collections.unmodifiableMap(copy);

        log.debug("[RuntimeProcess.<init>] RuntimeProcess initialized with {} stages", this.stages.size());
    }

    public void print() {
        log.debug("[RuntimeProcess.print] Printing runtime process");

        System.out.println("==== Runtime Process ====");
        stages.forEach((stage, stepMap) -> {
            log.debug("[RuntimeProcess.print] Stage: {} with {} steps", stage, stepMap.size());
            System.out.println("Stage: " + stage);
            stepMap.forEach((step, variable) -> {
                log.trace("[RuntimeProcess.print] Step: {} -> Variable: {}", step, variable);
                System.out.println("  Step: " + step + " -> Variable: " + variable);
            });
        });
        System.out.println("=========================");

        log.debug("[RuntimeProcess.print] Finished printing runtime process");
    }

}
