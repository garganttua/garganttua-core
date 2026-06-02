package com.garganttua.core.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.script.IScript;

/**
 * Stateless collection of workflow result variables and stage outputs from an
 * executed {@link IScript} (or its variables map, for the precompiled path),
 * plus the human-readable error dump. Extracted from {@link Workflow} to keep
 * that class focused on execution orchestration.
 */
final class WorkflowVariableCollector {
    private static final Logger log = Logger.getLogger(WorkflowVariableCollector.class);

    private WorkflowVariableCollector() {}

    static Map<String, Object> collectVariables(IScript script, List<WorkflowStage> stagesToCollect) {
        Map<String, Object> variables = new HashMap<>();
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                String scriptName = sanitizeIdentifier(ws.getName() != null ? ws.getName() : "script");
                String resultVarName = "_" + stage.name() + "_" + scriptName + "_result";
                String codeVarName = "_" + stage.name() + "_" + scriptName + "_code";
                String refVarName = "_" + stage.name() + "_" + scriptName + "_ref";

                script.getVariable(resultVarName, IClass.getClass(Object.class))
                        .ifPresent(v -> variables.put(resultVarName, v));
                script.getVariable(codeVarName, IClass.getClass(Integer.class))
                        .ifPresent(v -> variables.put(codeVarName, v));
                script.getVariable(refVarName, IClass.getClass(Object.class))
                        .ifPresent(v -> variables.put(refVarName, v));

                // Collect output mappings
                for (String outputVar : ws.getOutputs().keySet()) {
                    script.getVariable(outputVar, IClass.getClass(Object.class))
                            .ifPresent(v -> variables.put(outputVar, v));
                }
            }
        }

        // Collect special variables
        script.getVariable("output", IClass.getClass(Object.class))
                .ifPresent(v -> variables.put("output", v));
        script.getVariable("code", IClass.getClass(Integer.class))
                .ifPresent(v -> variables.put("code", v));

        return variables;
    }

    static String sanitizeIdentifier(String name) {
        if (name == null) {
            return "script";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    static void logErrorDump(String workflowName, IScript script, String scriptSource,
                              List<WorkflowStage> stagesToCollect, String message, Throwable exception) {
        StringBuilder dump = new StringBuilder();
        dump.append("\n╔══════════════════════════════════════════════════════════════════════╗\n");
        dump.append("║  WORKFLOW ERROR DUMP                                                ║\n");
        dump.append("╠══════════════════════════════════════════════════════════════════════╣\n");
        dump.append("║  Workflow: ").append(workflowName).append("\n");
        dump.append("║  Error: ").append(message).append("\n");

        // Exception chain
        if (exception != null) {
            dump.append("║\n║  Exception chain:\n");
            Throwable t = exception;
            int depth = 0;
            while (t != null && depth < 10) {
                dump.append("║    ").append("  ".repeat(depth))
                    .append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append("\n");
                t = t.getCause();
                depth++;
            }
        }

        // Error context variables from script
        dump.append("║\n║  Script error context:\n");
        script.getVariable("_scriptErrorLine", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Line: ").append(v).append("\n"));
        script.getVariable("_scriptErrorSource", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Source: ").append(v).append("\n"));
        script.getVariable("_scriptErrorStep", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Step: ").append(v).append("\n"));
        script.getVariable("_scriptErrorType", IClass.getClass(Object.class))
                .ifPresent(v -> dump.append("║    Type: ").append(v).append("\n"));

        // Collected variables at failure point
        dump.append("║\n║  Variables at failure point:\n");
        Map<String, Object> vars = collectVariables(script, stagesToCollect);
        if (vars.isEmpty()) {
            dump.append("║    (none)\n");
        } else {
            for (var entry : vars.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue().toString() : "null";
                if (val.length() > 120) val = val.substring(0, 120) + "...";
                dump.append("║    ").append(entry.getKey()).append(" = ").append(val).append("\n");
            }
        }

        // Stages
        dump.append("║\n║  Stages: ");
        dump.append(stagesToCollect.stream().map(WorkflowStage::name).collect(Collectors.joining(" → ")));
        dump.append("\n");

        // Generated script (truncated)
        dump.append("║\n║  Generated script:\n");
        for (String line : scriptSource.split("\n")) {
            dump.append("║    ").append(line).append("\n");
        }

        dump.append("╚══════════════════════════════════════════════════════════════════════╝");
        log.error("{}", dump);
    }

    static Map<String, Object> collectStageOutputs(IScript script, List<WorkflowStage> stagesToCollect) {
        Map<String, Object> stageOutputs = new HashMap<>();
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                for (var output : ws.getOutputs().entrySet()) {
                    String key = stage.name() + "." + output.getKey();
                    Optional<Object> value = script.getVariable(output.getKey(), IClass.getClass(Object.class));
                    value.ifPresent(v -> stageOutputs.put(key, v));
                }
            }
        }
        return stageOutputs;
    }

    /**
     * Same as {@link #collectVariables(IScript, List)} but reads from a
     * variables map (as returned by {@link com.garganttua.core.script.IScriptExecutionResult#variables()}).
     * Used by the precompiled execution path.
     */
    static Map<String, Object> filterToCollectedVariables(Map<String, Object> source,
            List<WorkflowStage> stagesToCollect) {
        Map<String, Object> variables = new HashMap<>();
        if (source == null || source.isEmpty()) {
            return variables;
        }
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                String scriptName = sanitizeIdentifier(ws.getName() != null ? ws.getName() : "script");
                String resultVarName = "_" + stage.name() + "_" + scriptName + "_result";
                String codeVarName = "_" + stage.name() + "_" + scriptName + "_code";
                String refVarName = "_" + stage.name() + "_" + scriptName + "_ref";
                copyIfPresent(source, variables, resultVarName);
                copyIfPresent(source, variables, codeVarName);
                copyIfPresent(source, variables, refVarName);
                for (String outputVar : ws.getOutputs().keySet()) {
                    copyIfPresent(source, variables, outputVar);
                }
            }
        }
        copyIfPresent(source, variables, "output");
        copyIfPresent(source, variables, "code");
        return variables;
    }

    /** Mirror of {@link #collectStageOutputs(IScript, List)} for the precompiled path. */
    static Map<String, Object> collectStageOutputsFromMap(Map<String, Object> source,
            List<WorkflowStage> stagesToCollect) {
        Map<String, Object> stageOutputs = new HashMap<>();
        if (source == null || source.isEmpty()) {
            return stageOutputs;
        }
        for (WorkflowStage stage : stagesToCollect) {
            for (WorkflowScript ws : stage.scripts()) {
                for (var output : ws.getOutputs().entrySet()) {
                    String key = stage.name() + "." + output.getKey();
                    Object v = source.get(output.getKey());
                    if (v != null) {
                        stageOutputs.put(key, v);
                    }
                }
            }
        }
        return stageOutputs;
    }

    static void copyIfPresent(Map<String, Object> src, Map<String, Object> dst, String key) {
        Object v = src.get(key);
        if (v != null) {
            dst.put(key, v);
        }
    }
}
