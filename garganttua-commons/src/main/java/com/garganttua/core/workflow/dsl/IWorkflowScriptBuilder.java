package com.garganttua.core.workflow.dsl;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.workflow.WorkflowScript;
import com.garganttua.core.workflow.chaining.CodeAction;

public interface IWorkflowScriptBuilder extends ILinkedBuilder<IWorkflowStageBuilder, WorkflowScript> {

    IWorkflowScriptBuilder name(String name);

    /**
     * Sets a description for this script.
     * This description will be shown in workflow cartography.
     *
     * @param description the script description
     */
    IWorkflowScriptBuilder description(String description);

    IWorkflowScriptBuilder inline();

    IWorkflowScriptBuilder input(String scriptVar, String expression);

    IWorkflowScriptBuilder output(String workflowVar, String scriptVar);

    IWorkflowScriptBuilder onCode(int code, CodeAction action);

    /**
     * Adds a catch clause for immediate exceptions (! syntax).
     * Catches exceptions thrown directly by this script.
     *
     * @param expression the handler expression (e.g., "handleError(@exception)")
     */
    IWorkflowScriptBuilder catch_(String expression);

    /**
     * Adds a downstream catch clause (* syntax).
     * Catches exceptions propagated from nested/downstream calls.
     *
     * @param expression the handler expression (e.g., "handleDownstreamError(@exception)")
     */
    IWorkflowScriptBuilder catchDownstream(String expression);
}
