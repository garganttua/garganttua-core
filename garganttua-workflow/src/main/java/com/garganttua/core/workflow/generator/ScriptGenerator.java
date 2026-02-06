package com.garganttua.core.workflow.generator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.garganttua.core.workflow.WorkflowException;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.WorkflowStage;
import com.garganttua.core.workflow.chaining.CodeAction;

public class ScriptGenerator {

    public String generate(String workflowName, List<WorkflowStage> stages, Map<String, Object> presetVariables)
            throws WorkflowException {
        return generate(workflowName, stages, presetVariables, false);
    }

    public String generate(String workflowName, List<WorkflowStage> stages, Map<String, Object> presetVariables,
            boolean inlineAll) throws WorkflowException {
        StringBuilder script = new StringBuilder();

        appendHeader(script, workflowName);
        appendPresetVariables(script, presetVariables);

        for (WorkflowStage stage : stages) {
            appendStage(script, stage, inlineAll);
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

    private void appendStage(StringBuilder script, WorkflowStage stage, boolean inlineAll) throws WorkflowException {
        script.append("# Stage: ").append(stage.name()).append("\n");

        boolean hasWrap = stage.hasWrap();
        boolean hasCatch = stage.hasCatch();

        if (hasWrap || hasCatch) {
            // Generate stage content into a temporary buffer
            StringBuilder stageContent = new StringBuilder();
            for (WorkflowScript ws : stage.scripts()) {
                appendScript(stageContent, stage.name(), ws, inlineAll);
            }

            // Apply wrapper if present
            if (hasWrap) {
                // Wrap the stage content: the wrap expression should use @0 or similar placeholder
                String wrapExpr = stage.wrapExpression();
                script.append("_").append(stage.name()).append("_result <- ");
                script.append(wrapExpr.replace("@0", "(\n" + indent(stageContent.toString(), "    ") + ")"));
            } else {
                // Just group the content
                script.append("_").append(stage.name()).append("_result <- (\n");
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
                appendScript(script, stage.name(), ws, inlineAll);
            }
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

    private void appendScript(StringBuilder script, String stageName, WorkflowScript ws, boolean inlineAll)
            throws WorkflowException {
        String scriptName = sanitizeIdentifier(ws.getName() != null ? ws.getName() : "script");

        // Input mappings
        for (var input : ws.getInputs().entrySet()) {
            script.append(input.getKey())
                  .append(" <- ").append(input.getValue()).append("\n");
        }

        // Determine if this script should be inlined
        boolean shouldInline = inlineAll || ws.isInline() || !ws.isFile();

        // Script execution
        if (ws.isFile() && !shouldInline) {
            // File -> include() + execute_script() + script_variable() pattern
            String refVarName = "_" + stageName + "_" + scriptName + "_ref";
            String codeVarName = "_" + stageName + "_" + scriptName + "_code";

            // Include: loads, compiles, returns script name
            script.append(refVarName).append(" <- ");
            script.append("include(\"").append(escapeString(ws.getPath())).append("\")\n");

            // Execute with input variables as positional arguments
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
        } else {
            // Inline script (string or file with inline()) - insert content directly
            String content = ws.loadContent();

            // Replace positional variables (@0, @1, ...) with named input variables
            content = replacePositionalVariables(content, ws.getInputs());

            // Add each line of the script content, stripping comments and shebang
            for (String line : content.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("//")) {
                    continue;
                }
                script.append(line).append("\n");
            }

            // For inline scripts, output mappings copy script variables to workflow variables
            for (var output : ws.getOutputs().entrySet()) {
                String workflowVar = output.getKey();
                String scriptVar = output.getValue();
                // Only generate mapping if workflow var differs from script var
                if (!workflowVar.equals(scriptVar)) {
                    script.append(workflowVar).append(" <- @").append(scriptVar).append("\n");
                }
            }
        }

        // Code actions for inline scripts (include case handles them as pipe clauses on execute_script)
        if (shouldInline || !ws.isFile()) {
            for (var codeAction : ws.getCodeActions().entrySet()) {
                if (codeAction.getValue() != CodeAction.CONTINUE) {
                    script.append("@code == ").append(codeAction.getKey())
                          .append(" | ").append(codeAction.getValue().toScript()).append("\n");
                }
            }
        }

        script.append("\n");
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
