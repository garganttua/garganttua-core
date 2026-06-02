package com.garganttua.core.runtime;

import com.garganttua.core.observability.Logger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable view of a runtime's stage/step structure, keyed by stage name then
 * step name to its bound variable.
 *
 * <p>Used to render a human-readable summary of how a runtime's steps are
 * organized into stages.</p>
 */
public class RuntimeProcess {
    private static final Logger log = Logger.getLogger(RuntimeProcess.class);

    private final Map<String, Map<String, String>> stages;

    /**
     * Creates a runtime process from the given stage map. The supplied map and
     * its nested maps are defensively copied and made unmodifiable.
     *
     * @param stages stage name to (step name to variable name) mapping
     */
    public RuntimeProcess(Map<String, Map<String, String>> stages) {
        log.trace("[RuntimeProcess.<init>] Initializing RuntimeProcess with stages={}", stages);

        Map<String, Map<String, String>> copy = new LinkedHashMap<>();
        stages.forEach((stage, stepMap) -> copy.put(stage, Collections.unmodifiableMap(new LinkedHashMap<>(stepMap))));
        this.stages = Collections.unmodifiableMap(copy);

        log.debug("[RuntimeProcess.<init>] RuntimeProcess initialized with {} stages", this.stages.size());
    }

    /**
     * Emits a human-readable rendering of the stages and their steps to standard output.
     */
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
