package com.garganttua.core.workflow.dsl;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.chaining.CodeAction;

/**
 * Fluent builder for a single {@link WorkflowScript} inside a stage.
 *
 * <p>Configures the script's name, condition, inputs/outputs, exit-code
 * routing and catch clauses. Call {@code up()} to return to the owning
 * {@link IWorkflowStageBuilder}.
 */
public interface IWorkflowScriptBuilder extends ILinkedBuilder<IWorkflowStageBuilder, WorkflowScript> {

    /**
     * Sets the script's logical name.
     *
     * @param name the script name
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder name(String name);

    /**
     * Sets a condition for this script. The script will only be executed
     * if the condition expression evaluates to true at runtime.
     *
     * @param expression the Garganttua expression to evaluate (e.g., "equals(@env, \"prod\")")
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder when(String expression);

    /**
     * Sets a description for this script.
     * This description will be shown in workflow cartography.
     *
     * @param description the script description
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder description(String description);

    /**
     * Forces this script to be inlined into the generated workflow script
     * rather than referenced via {@code include()}.
     *
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder inline();

    /**
     * Maps a workflow-level expression into a script variable.
     *
     * @param scriptVar  the script variable name to populate
     * @param expression the expression producing its value
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder input(String scriptVar, String expression);

    /**
     * Exports a script variable back to the workflow scope.
     *
     * @param workflowVar the workflow variable name to populate
     * @param scriptVar   the script variable to read from
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder output(String workflowVar, String scriptVar);

    /**
     * Routes a specific script exit code to a {@link CodeAction}.
     *
     * @param code   the exit code to react to
     * @param action the action to apply
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder onCode(int code, CodeAction action);

    /**
     * Adds a catch clause for immediate exceptions (! syntax).
     * Catches exceptions thrown directly by this script.
     *
     * @param expression the handler expression (e.g., "handleError(@exception)")
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder catch_(String expression);

    /**
     * Adds a downstream catch clause (* syntax).
     * Catches exceptions propagated from nested/downstream calls.
     *
     * @param expression the handler expression (e.g., "handleDownstreamError(@exception)")
     * @return this builder for method chaining
     */
    IWorkflowScriptBuilder catchDownstream(String expression);
}
