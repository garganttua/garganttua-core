package com.garganttua.core.workflow.dsl;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.workflow.WorkflowStage;

public interface IWorkflowStageBuilder extends ILinkedBuilder<IWorkflowBuilder, WorkflowStage> {

    IWorkflowScriptBuilder script(String content);

    IWorkflowScriptBuilder script(File file);

    IWorkflowScriptBuilder script(Path path);

    IWorkflowScriptBuilder script(InputStream inputStream);

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
