package com.garganttua.core.workflow.dsl;

import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.workflow.IWorkflow;
import com.garganttua.core.workflow.WorkflowTimingConfig;

/**
 * Builder interface for constructing {@link IWorkflow} instances.
 *
 * <p>
 * {@code IWorkflowBuilder} extends {@link IDependentBuilder} to support automatic
 * dependency injection of {@code IInjectionContext} and {@code IExpressionContext}
 * through the {@code provide()} method.
 * </p>
 *
 * <h2>Dependency Management</h2>
 * <p>
 * The builder declares dependencies on:
 * </p>
 * <ul>
 *   <li>{@code IInjectionContextBuilder} - required for bean resolution during script execution</li>
 *   <li>{@code IExpressionContextBuilder} - required for expression evaluation</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IWorkflow workflow = WorkflowBuilder.create()
 *     .provide(injectionContextBuilder)
 *     .provide(expressionContextBuilder)
 *     .name("my-workflow")
 *     .stage("validation")
 *         .script("result <- validate(@input)")
 *             .up()
 *         .up()
 *     .build();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IWorkflow
 * @see IDependentBuilder
 */
public interface IWorkflowBuilder extends IDependentBuilder<IWorkflowBuilder, IWorkflow> {

    /**
     * Navigate back to the {@link IWorkflowsBuilder} that opened this child.
     *
     * @return the parent workflows builder
     * @throws IllegalStateException if this builder was created outside of a
     *                               {@code WorkflowsBuilder} context
     */
    IWorkflowsBuilder up();

    /**
     * Sets the workflow name.
     *
     * @param name the workflow name
     * @return this builder for method chaining
     */
    IWorkflowBuilder name(String name);

    /**
     * Sets a preset variable that will be available during workflow execution.
     *
     * @param name  the variable name
     * @param value the variable value
     * @return this builder for method chaining
     */
    IWorkflowBuilder variable(String name, Object value);

    /**
     * Forces all scripts to be inlined in the generated script,
     * regardless of their individual inline settings.
     * Useful for debugging or when include() is not available.
     *
     * @return this builder for method chaining
     */
    IWorkflowBuilder inlineAll();

    /**
     * Pre-compile the generated workflow script at build time so each
     * {@code execute()} call reuses the same thread-safe
     * {@link com.garganttua.core.script.ICompiledScript} instead of
     * re-parsing the script and re-building its runtime every time.
     *
     * <p>Significant speedup for workflows executed repeatedly (per-request
     * handlers, batch loops). Safe under concurrent {@code execute()} calls —
     * the underlying compiled handle is immutable and each call gets its own
     * runtime context.
     *
     * <p>Workflows that use {@code WorkflowExecutionOptions} filtering keep
     * spawning a fresh script per call regardless of this flag, because the
     * generated source changes per invocation when filtering is active.
     *
     * <p>Default: {@code false} (legacy behaviour — fresh script per
     * execution).
     *
     * @param enabled whether to pre-compile
     * @return this builder for method chaining
     * @since 2.0.0-ALPHA02
     */
    IWorkflowBuilder precompile(boolean enabled);

    /**
     * Creates a new stage builder for the workflow.
     *
     * @param name the stage name
     * @return a new stage builder
     */
    IWorkflowStageBuilder stage(String name);

    /**
     * Configures emission of observability timing markers in the generated
     * script. Defaults to {@link WorkflowTimingConfig#disabled()} — when not
     * set, the generated script is byte-identical to a workflow built without
     * this call.
     *
     * @param config the timing config (must not be {@code null})
     * @return this builder for method chaining
     * @since 2.0.0-ALPHA02
     */
    IWorkflowBuilder timing(WorkflowTimingConfig config);

    /**
     * Returns a textual representation of the workflow structure.
     * This can be called before build() to preview the workflow.
     *
     * @return a formatted string showing the workflow structure
     */
    String describeWorkflow();

    /**
     * Returns the workflow structure as a data object.
     * This can be called before build() to get structured information.
     *
     * @return the workflow descriptor containing all configuration
     */
    WorkflowDescriptor getDescriptor();
}
