package com.garganttua.core.workflow.dsl;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.workflow.WorkflowStage;

/**
 * Fluent builder for a {@link WorkflowStage} within an {@link IWorkflowBuilder}.
 *
 * <p>Adds scripts from several source kinds and configures the stage-level
 * condition, wrap and catch expressions. Call {@code up()} to return to the
 * owning workflow builder.
 */
public interface IWorkflowStageBuilder extends ILinkedBuilder<IWorkflowBuilder, WorkflowStage> {

    /**
     * Sets a condition for this stage. All scripts in the stage will only be
     * executed if the condition expression evaluates to true at runtime.
     *
     * @param expression the Garganttua expression to evaluate (e.g., "equals(@env, \"prod\")")
     * @return this builder for method chaining
     */
    IWorkflowStageBuilder when(String expression);

    /**
     * Adds an inline script from its source text.
     *
     * @param content the script source (or a {@code classpath:} reference)
     * @return a script builder for the new script
     */
    IWorkflowScriptBuilder script(String content);

    /**
     * Adds a script loaded from a file.
     *
     * @param file the script file
     * @return a script builder for the new script
     */
    IWorkflowScriptBuilder script(File file);

    /**
     * Adds a script loaded from a path.
     *
     * @param path the script path
     * @return a script builder for the new script
     */
    IWorkflowScriptBuilder script(Path path);

    /**
     * Adds a script read from an input stream.
     *
     * @param inputStream the stream supplying the script source
     * @return a script builder for the new script
     */
    IWorkflowScriptBuilder script(InputStream inputStream);

    /**
     * Adds a script read from a reader.
     *
     * @param reader the reader supplying the script source
     * @return a script builder for the new script
     */
    IWorkflowScriptBuilder script(Reader reader);

    /**
     * Wraps the entire stage in an expression.
     * The stage content will be passed as the first argument to the wrapper function.
     *
     * <p>Example: {@code .wrap("retry(3, @0)")} will wrap the stage in a retry expression
     * that retries up to 3 times on failure.</p>
     *
     * @param expression the wrapper expression (use @0 to reference the stage content)
     * @return this builder for method chaining
     */
    IWorkflowStageBuilder wrap(String expression);

    /**
     * Adds a catch clause for exceptions occurring in this stage.
     *
     * @param expression the handler expression (e.g., "handleError(@exception)")
     * @return this builder for method chaining
     */
    IWorkflowStageBuilder catch_(String expression);

    /**
     * Adds a downstream catch clause for exceptions propagated from nested calls.
     *
     * @param expression the handler expression
     * @return this builder for method chaining
     */
    IWorkflowStageBuilder catchDownstream(String expression);
}
