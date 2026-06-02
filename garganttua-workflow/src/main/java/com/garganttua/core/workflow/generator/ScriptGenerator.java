package com.garganttua.core.workflow.generator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import java.util.concurrent.atomic.AtomicBoolean;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.WorkflowTimingConfig;
import com.garganttua.core.workflow.chaining.CodeAction;
import com.garganttua.core.workflow.header.ScriptHeaderParser;

/**
 * Converts workflow stage definitions into Garganttua Script source code.
 *
 * <p>Supports two script modes:
 * <ul>
 *   <li><b>Include mode</b> (file-based): generates {@code include()} + {@code execute_script()}
 *       + {@code script_variable()} calls. Each script runs in an isolated child context.</li>
 *   <li><b>Inline mode</b>: embeds script content directly. Each inline script is wrapped
 *       in a {@code (...)} statement group for function scope isolation, preventing
 *       name collisions between stages.</li>
 * </ul>
 *
 * <p>Conditional execution uses {@code if(condition, block)} (two-argument form)
 * for lazy evaluation of statement blocks. When the condition is false, {@code if()}
 * returns null and the null guard in variable assignment skips the write.
 * Stage-level and script-level conditions are combined with
 * {@code and()} when both are present.
 */
public class ScriptGenerator {

    private static final Logger log = Logger.getLogger(ScriptGenerator.class);

    private static final ScriptHeaderParser HEADER_PARSER = new ScriptHeaderParser();

    /**
     * Whether {@code garganttua-observability} is on the classpath. That module
     * (an <b>optional</b> dependency) provides the script-side {@code observe(...)}
     * expression function. When it is absent, timing instrumentation is silently
     * skipped so generated scripts compile and run without it.
     */
    private static final boolean OBSERVABILITY_AVAILABLE = isObservabilityAvailable();

    private static final AtomicBoolean ABSENCE_LOGGED = new AtomicBoolean(false);

    private static boolean isObservabilityAvailable() {
        try {
            Class.forName("com.garganttua.core.observability.ObservabilityExpressions");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Generates script source in include mode (file-backed scripts) with default options.
     *
     * @param workflowName    the workflow name (emitted as a header comment)
     * @param stages          the ordered workflow stages
     * @param presetVariables the preset variables to seed at the top of the script
     * @return the generated Garganttua Script source
     * @throws WorkflowException if a stage or script cannot be rendered
     */
    public String generate(String workflowName, List<WorkflowStage> stages, Map<String, Object> presetVariables)
            throws WorkflowException {
        return generate(workflowName, stages, presetVariables, false, ScriptGenerationOptions.defaults());
    }

    /**
     * Generates script source with default options and the given inline mode.
     *
     * @param workflowName    the workflow name (emitted as a header comment)
     * @param stages          the ordered workflow stages
     * @param presetVariables the preset variables to seed at the top of the script
     * @param inlineAll       when {@code true}, embeds all script content inline instead of {@code include()}
     * @return the generated Garganttua Script source
     * @throws WorkflowException if a stage or script cannot be rendered
     */
    public String generate(String workflowName, List<WorkflowStage> stages, Map<String, Object> presetVariables,
            boolean inlineAll) throws WorkflowException {
        return generate(workflowName, stages, presetVariables, inlineAll, ScriptGenerationOptions.defaults());
    }

    /**
     * Generates script source for the given stages.
     *
     * <p>Timing instrumentation requested via {@code options} is silently skipped when
     * {@code garganttua-observability} is absent from the classpath, so the generated
     * script compiles either way.
     *
     * @param workflowName    the workflow name (emitted as a header comment)
     * @param stages          the ordered workflow stages
     * @param presetVariables the preset variables to seed at the top of the script
     * @param inlineAll       when {@code true}, embeds all script content inline instead of {@code include()}
     * @param options         generation options (timing config); {@code null} falls back to disabled timing
     * @return the generated Garganttua Script source
     * @throws WorkflowException if a stage or script cannot be rendered
     */
    public String generate(String workflowName, List<WorkflowStage> stages, Map<String, Object> presetVariables,
            boolean inlineAll, ScriptGenerationOptions options) throws WorkflowException {
        StringBuilder script = new StringBuilder();
        WorkflowTimingConfig timing = options == null ? WorkflowTimingConfig.disabled() : options.timing();

        // Observability is an optional dependency: if its module (and thus the
        // script-side observe(...) function) is absent, emit no instrumentation.
        if (!OBSERVABILITY_AVAILABLE && !timing.isFullyDisabled()) {
            if (ABSENCE_LOGGED.compareAndSet(false, true)) {
                log.debug("Workflow timing requested but garganttua-observability is not on the "
                        + "classpath; skipping observe(...) instrumentation");
            }
            timing = WorkflowTimingConfig.disabled();
        }

        appendHeader(script, workflowName);
        appendPresetVariables(script, presetVariables);

        for (WorkflowStage stage : stages) {
            appendStage(script, stage, inlineAll, timing);
        }

        appendOutput(script, stages);

        return script.toString();
    }

    private void appendHeader(StringBuilder script, String workflowName) {
        script.append("# Workflow: ").append(workflowName).append("\n");
        script.append("# Generated: ").append(Instant.now()).append("\n\n");
    }

    private void appendPresetVariables(StringBuilder script, Map<String, Object> presetVariables) {
        if (presetVariables == null || presetVariables.isEmpty()) {
            return;
        }
        script.append("# Preset variables\n");
        for (var entry : presetVariables.entrySet()) {
            script.append(entry.getKey()).append(" <- ")
                  .append(formatValue(entry.getValue())).append("\n");
        }
        script.append("\n");
    }

    private void appendStage(StringBuilder script, WorkflowStage stage, boolean inlineAll,
            WorkflowTimingConfig timing) throws WorkflowException {
        String stageName = sanitizeIdentifier(stage.name());
        script.append("# Stage: ").append(stage.name()).append("\n");

        // Emit stage condition guard variable if present
        String stageCondition = stage.condition();
        if (stageCondition != null && !stageCondition.isEmpty()) {
            script.append("_").append(stageName).append("_cond <- ").append(stageCondition).append("\n");
        }

        boolean emitStageTiming = timing.isStageEnabled(stage.name());
        if (emitStageTiming) {
            script.append("observe(\"start\", \"stage:").append(escapeString(stage.name())).append("\")\n");
        }

        boolean hasWrap = stage.hasWrap();
        boolean hasCatch = stage.hasCatch();

        if (hasWrap || hasCatch) {
            // Generate stage content into a temporary buffer
            StringBuilder stageContent = new StringBuilder();
            for (WorkflowScript ws : stage.scripts()) {
                appendScript(stageContent, stageName, ws, inlineAll, stageCondition, stage.name(), timing);
            }

            // Apply wrapper if present
            if (hasWrap) {
                // Wrap the stage content: the wrap expression should use @0 or similar placeholder
                String wrapExpr = stage.wrapExpression();
                script.append("_").append(stageName).append("_result <- ");
                script.append(wrapExpr.replace("@0", "(\n" + indent(stageContent.toString(), "    ") + ")"));
            } else {
                // Just group the content
                script.append("_").append(stageName).append("_result <- (\n");
                script.append(indent(stageContent.toString(), "    "));
                script.append(")");
            }

            // Stage-level catch clauses
            if (stage.catchExpression() != null && !stage.catchExpression().isEmpty()) {
                script.append("\n    ! => ").append(stage.catchExpression());
            }
            if (stage.catchDownstreamExpression() != null && !stage.catchDownstreamExpression().isEmpty()) {
                script.append("\n    * => ").append(stage.catchDownstreamExpression());
            }

            script.append("\n");
        } else {
            // No wrap or catch - emit scripts directly
            for (WorkflowScript ws : stage.scripts()) {
                appendScript(script, stageName, ws, inlineAll, stageCondition, stage.name(), timing);
            }
        }

        if (emitStageTiming) {
            script.append("observe(\"end\", \"stage:").append(escapeString(stage.name())).append("\")\n");
        }

        script.append("\n");
    }

    private String indent(String content, String indentation) {
        StringBuilder result = new StringBuilder();
        for (String line : content.split("\n")) {
            if (!line.isEmpty()) {
                result.append(indentation).append(line).append("\n");
            } else {
                result.append("\n");
            }
        }
        return result.toString();
    }

    private void appendScript(StringBuilder script, String stageName, WorkflowScript ws, boolean inlineAll,
            String stageCondition, String rawStageName, WorkflowTimingConfig timing) throws WorkflowException {
        String scriptName = sanitizeIdentifier(ws.getName() != null ? ws.getName() : "script");
        String rawScriptName = ws.getName() != null ? ws.getName() : "script";

        boolean emitScriptTiming = timing.isScriptEnabled(rawStageName, rawScriptName);
        String scriptSource = rawStageName + "." + rawScriptName;
        if (emitScriptTiming) {
            script.append("observe(\"start\", \"script:").append(escapeString(scriptSource)).append("\")\n");
        }

        // Input mappings (always emitted — needed for condition evaluation)
        for (var input : ws.getInputs().entrySet()) {
            script.append(input.getKey())
                  .append(" <- ").append(input.getValue()).append("\n");
        }

        // Compute effective condition
        boolean hasStageCondition = stageCondition != null && !stageCondition.isEmpty();
        boolean hasScriptCondition = ws.getCondition() != null && !ws.getCondition().isEmpty();

        String effectiveCondition = null;
        if (hasStageCondition && hasScriptCondition) {
            effectiveCondition = "and(@_" + stageName + "_cond, " + ws.getCondition() + ")";
        } else if (hasStageCondition) {
            effectiveCondition = "@_" + stageName + "_cond";
        } else if (hasScriptCondition) {
            effectiveCondition = ws.getCondition();
        }

        boolean isConditional = effectiveCondition != null;

        // Emit condition variable if conditional
        String condVarName = "_" + stageName + "_" + scriptName + "_cond";
        if (isConditional) {
            script.append(condVarName).append(" <- ").append(effectiveCondition).append("\n");
        }

        // Determine if this script should be inlined
        boolean shouldInline = inlineAll || ws.isInline() || !ws.isFile();

        // Script execution
        if (ws.isFile() && !shouldInline) {
            appendIncludedExecution(script, stageName, scriptName, ws, condVarName, isConditional);
        } else {
            appendInlineExecution(script, condVarName, ws, isConditional);
        }

        if (emitScriptTiming) {
            String codeVarName = "_" + stageName + "_" + scriptName + "_code";
            script.append("observe(\"end\", \"script:").append(escapeString(scriptSource))
                  .append("\", @").append(codeVarName).append(")\n");
        }

        script.append("\n");
    }

    /** File-backed script: {@code include()} then conditional or unconditional {@code execute_script()}. */
    private void appendIncludedExecution(StringBuilder script, String stageName, String scriptName,
            WorkflowScript ws, String condVarName, boolean isConditional) {
        // File -> include() + execute_script() + script_variable() pattern
        String refVarName = "_" + stageName + "_" + scriptName + "_ref";
        String codeVarName = "_" + stageName + "_" + scriptName + "_code";

        // Include: loads, compiles, returns script name (unconditional — lightweight)
        script.append(refVarName).append(" <- ");
        script.append("include(\"").append(escapeString(ws.getPath())).append("\")\n");

        if (isConditional) {
            appendConditionalInclude(script, refVarName, codeVarName, condVarName, ws);
        } else {
            appendUnconditionalInclude(script, refVarName, codeVarName, ws);
        }
    }

    /**
     * Conditional execution: group execute_script + output mappings in a single if() lazy
     * block. When the condition is false nothing inside executes — no side effects on outputs.
     */
    private void appendConditionalInclude(StringBuilder script, String refVarName, String codeVarName,
            String condVarName, WorkflowScript ws) {
        script.append("if(@").append(condVarName).append(", (\n");

        // Execute the script
        script.append("    ").append(codeVarName).append(" <- execute_script(@").append(refVarName);
        for (var input : ws.getInputs().entrySet()) {
            script.append(", @").append(input.getKey());
        }
        script.append(")\n");

        // Code actions inside the block
        for (var codeAction : ws.getCodeActions().entrySet()) {
            if (codeAction.getValue() != CodeAction.CONTINUE) {
                script.append("    if(equals(@").append(codeVarName).append(", ")
                      .append(codeAction.getKey()).append("), (")
                      .append(codeAction.getValue().toScript())
                      .append("))\n");
            }
        }

        // Output mappings inside the block
        for (var output : ws.getOutputs().entrySet()) {
            script.append("    ").append(output.getKey())
                  .append(" <- script_variable(@").append(refVarName)
                  .append(", \"").append(escapeString(output.getValue()))
                  .append("\")\n");
        }

        script.append("))\n");
    }

    /** Unconditional execution: execute_script with catch/pipe clauses and script_variable() outputs. */
    private void appendUnconditionalInclude(StringBuilder script, String refVarName, String codeVarName,
            WorkflowScript ws) {
        script.append(codeVarName).append(" <- ");
        script.append("execute_script(@").append(refVarName);
        for (var input : ws.getInputs().entrySet()) {
            script.append(", @").append(input.getKey());
        }
        script.append(")");

        // Catch clauses on execute_script statement
        if (ws.getCatchExpression() != null && !ws.getCatchExpression().isEmpty()) {
            script.append("\n    ! => ").append(ws.getCatchExpression());
        }
        if (ws.getCatchDownstreamExpression() != null && !ws.getCatchDownstreamExpression().isEmpty()) {
            script.append("\n    * => ").append(ws.getCatchDownstreamExpression());
        }

        // Code actions as pipe clauses on execute_script statement
        for (var codeAction : ws.getCodeActions().entrySet()) {
            if (codeAction.getValue() != CodeAction.CONTINUE) {
                script.append("\n    | equals(@").append(codeVarName).append(", ")
                      .append(codeAction.getKey()).append(") => ")
                      .append(codeAction.getValue().toScript());
            }
        }

        script.append("\n");

        // Output mappings using script_variable()
        for (var output : ws.getOutputs().entrySet()) {
            script.append(output.getKey())
                  .append(" <- script_variable(@").append(refVarName)
                  .append(", \"").append(escapeString(output.getValue())).append("\")\n");
        }
    }

    /** Inline script (string, or file with inline()): insert content directly into the generated script. */
    private void appendInlineExecution(StringBuilder script, String condVarName, WorkflowScript ws,
            boolean isConditional) {
        String content = ws.loadContent();

        // Strip #@workflow header if present — it's metadata, not executable code
        content = HEADER_PARSER.stripHeader(content);

        // Replace positional variables (@0, @1, ...) with named input variables
        content = replacePositionalVariables(content, ws.getInputs());

        if (isConditional) {
            // Conditional inline via if() with block
            script.append("if(@").append(condVarName).append(", (\n");
            appendInlineContentLines(script, content);
            appendInlineOutputMappings(script, ws);
            script.append("))\n");
        } else {
            // Unconditional inline — wrap in a group for function scope isolation
            script.append("(\n");
            appendInlineContentLines(script, content);
            appendInlineOutputMappings(script, ws);
            script.append(")\n");

            // Code actions for inline scripts (outside the group)
            for (var codeAction : ws.getCodeActions().entrySet()) {
                if (codeAction.getValue() != CodeAction.CONTINUE) {
                    script.append("@code == ").append(codeAction.getKey())
                          .append(" | ").append(codeAction.getValue().toScript()).append("\n");
                }
            }
        }
    }

    /** Emit inline content lines (4-space indented), skipping blanks and comment-only lines. */
    private void appendInlineContentLines(StringBuilder script, String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                continue;
            }
            script.append("    ").append(line).append("\n");
        }
    }

    /** Emit output mappings inside an inline block/group (4-space indented), skipping identity mappings. */
    private void appendInlineOutputMappings(StringBuilder script, WorkflowScript ws) {
        for (var output : ws.getOutputs().entrySet()) {
            String workflowVar = output.getKey();
            String scriptVar = output.getValue();
            if (!workflowVar.equals(scriptVar)) {
                script.append("    ").append(workflowVar)
                      .append(" <- @").append(scriptVar).append("\n");
            }
        }
    }

    private void appendOutput(StringBuilder script, List<WorkflowStage> stages) {
        script.append("# Output\n");
        // The final @output will be set by the last output mapping or can be explicitly set
        // For now, we don't auto-set output - the user controls this via output mappings
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        // For complex objects, we reference by variable
        return value.toString();
    }

    /**
     * Sanitizes a name to be a valid script identifier by replacing
     * non-alphanumeric characters (like hyphens) with underscores.
     */
    private String sanitizeIdentifier(String name) {
        if (name == null) {
            return "script";
        }
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private String escapeString(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Replaces positional variable references (@0, @1, ...) with named variables
     * based on the order of input mappings.
     *
     * @param content the script content
     * @param inputs  the input mappings (ordered)
     * @return content with positional references replaced by named variables
     */
    private String replacePositionalVariables(String content, Map<String, String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return content;
        }

        String result = content;
        int position = 0;
        for (String inputName : inputs.keySet()) {
            // Replace @0, @1, etc. with @inputName
            // Use word boundary to avoid replacing @0 in @01 or @0abc
            String pattern = "@" + position + "(?![0-9a-zA-Z_])";
            result = result.replaceAll(pattern, "@" + inputName);
            position++;
        }
        return result;
    }
}
