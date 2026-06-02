package com.garganttua.core.workflow.annotations;

/**
 * Contract for auto-detected workflow definitions.
 *
 * <p>A class annotated with {@link WorkflowDefinition} implements this
 * interface and uses the supplied builder to declare its stages and scripts.
 * {@code WorkflowsBuilder} opens a child workflow builder named after
 * {@link WorkflowDefinition#name()}, invokes {@link #define(Object)}, and
 * collects the resulting workflow into its registry.
 *
 * <p>The builder argument is typed as {@code Object} here to avoid pulling the
 * workflow DSL types into {@code garganttua-commons}; implementations cast to
 * {@code com.garganttua.core.workflow.dsl.IWorkflowBuilder}.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IWorkflowDefinition {

    /**
     * Configure the supplied workflow builder. The builder is already named
     * (from {@link WorkflowDefinition#name()}) and has its Bootstrap-injected
     * dependencies; the implementation only adds stages/scripts/variables.
     *
     * @param workflowBuilder the workflow builder to populate, passed as
     *                        {@code Object} and cast by the implementation to
     *                        {@code com.garganttua.core.workflow.dsl.IWorkflowBuilder}
     */
    void define(Object workflowBuilder);
}
