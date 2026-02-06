package com.garganttua.core.workflow;

/**
 * Interface for workflow execution.
 *
 * <p>
 * A workflow encapsulates a pre-generated script that can be executed with
 * input parameters. The script is generated during the build phase by the
 * {@link com.garganttua.core.workflow.dsl.WorkflowBuilder}.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IWorkflow {

    /**
     * Returns the workflow name.
     *
     * @return the workflow name
     */
    String getName();

    /**
     * Executes the workflow with the given input.
     *
     * @param input the workflow input containing payload and parameters
     * @return the workflow execution result
     */
    WorkflowResult execute(WorkflowInput input);

    /**
     * Executes the workflow with the given input and execution options.
     *
     * <p>
     * The options allow filtering which stages are executed, e.g. starting from
     * a specific stage, stopping after a stage, or skipping stages.
     * </p>
     *
     * @param input   the workflow input containing payload and parameters
     * @param options the execution options for stage filtering
     * @return the workflow execution result
     */
    WorkflowResult execute(WorkflowInput input, WorkflowExecutionOptions options);

    /**
     * Executes the workflow with empty input.
     *
     * @return the workflow execution result
     */
    WorkflowResult execute();

    /**
     * Returns the pre-generated script that this workflow executes.
     * The script is generated during the build phase.
     *
     * @return the generated script
     */
    String getGeneratedScript();
}
