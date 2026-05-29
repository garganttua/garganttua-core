package com.garganttua.core.workflow.dsl;

import java.util.Map;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder for a registry of named {@link IWorkflow workflows}.
 *
 * <p>Mirrors {@code IRuntimesBuilder} for the workflow engine: opens child
 * {@link IWorkflowBuilder} instances via {@link #workflow(String)}, auto-
 * detects classes annotated with {@code @WorkflowDefinition} from the
 * injection context, and builds the whole registry in one shot.
 *
 * <p>This is the SPI-discoverable entry point for the workflow module — a
 * {@code Bootstrap.autoDetect(true).load()} surface this builder via the
 * {@code IBootstrapBuilderFactory} ServiceLoader contract. Crucially,
 * {@code build()} succeeds with zero workflows: a freshly-discovered
 * {@code WorkflowsBuilder} on a Bootstrap that doesn't actually use workflows
 * must not block the rest of the build pipeline.
 *
 * @since 2.0.0-ALPHA02
 */
@Reflected
public interface IWorkflowsBuilder
        extends IAutomaticBuilder<IWorkflowsBuilder, Map<String, IWorkflow>>,
                IObservableBuilder<IWorkflowsBuilder, Map<String, IWorkflow>>,
                IPackageableBuilder<IWorkflowsBuilder, Map<String, IWorkflow>>,
                IDependentBuilder<IWorkflowsBuilder, Map<String, IWorkflow>> {

    /**
     * Open (or retrieve) the workflow builder named {@code name}. Repeated
     * calls with the same name return the same child builder.
     *
     * @param name the workflow's logical name (must be non-blank)
     * @return the child builder; call {@code .up()} to return here
     */
    IWorkflowBuilder workflow(String name);
}
