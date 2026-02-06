package com.garganttua.core.workflow.dsl;

import java.util.List;
import java.util.Map;

/**
 * Data structure describing a workflow's configuration.
 *
 * <p>
 * WorkflowDescriptor provides a structured representation of a workflow
 * including its name, stages, scripts, and variable mappings. It can be
 * used for introspection, documentation generation, or debugging.
 * </p>
 *
 * @param name            the workflow name
 * @param inlineAll       whether all scripts are forced inline
 * @param presetVariables the preset variables for the workflow
 * @param stages          the list of stage descriptors
 * @since 2.0.0-ALPHA01
 */
public record WorkflowDescriptor(
        String name,
        boolean inlineAll,
        Map<String, Object> presetVariables,
        List<StageDescriptor> stages) {

    /**
     * Describes a workflow stage.
     *
     * @param name                      the stage name
     * @param wrapExpression            the wrapper expression (e.g., "retry(3, @0)")
     * @param catchExpression           the stage-level catch expression
     * @param catchDownstreamExpression the stage-level downstream catch expression
     * @param scripts                   the list of script descriptors in this stage
     */
    public record StageDescriptor(
            String name,
            String wrapExpression,
            String catchExpression,
            String catchDownstreamExpression,
            List<ScriptDescriptor> scripts) {
    }

    /**
     * Describes a script within a stage.
     *
     * @param name                     the script name
     * @param description              the script description (from header or builder)
     * @param sourceType               the source type (STRING, FILE, PATH, etc.)
     * @param sourcePath               the source path (for file sources)
     * @param inline                   whether this script is inlined
     * @param inputMappings            the input variable mappings (scriptVar -> expression)
     * @param outputMappings           the output variable mappings (workflowVar -> scriptVar)
     * @param catchExpression          the immediate catch expression (! syntax)
     * @param catchDownstreamExpression the downstream catch expression (* syntax)
     * @param codeActions              the code actions (code -> action)
     */
    public record ScriptDescriptor(
            String name,
            String description,
            String sourceType,
            String sourcePath,
            boolean inline,
            Map<String, String> inputMappings,
            Map<String, String> outputMappings,
            String catchExpression,
            String catchDownstreamExpression,
            Map<Integer, String> codeActions) {
    }
}
