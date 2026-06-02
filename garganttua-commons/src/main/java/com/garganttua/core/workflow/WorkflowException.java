package com.garganttua.core.workflow;

import com.garganttua.core.CoreException;

/**
 * Exception for workflow execution errors.
 *
 * <p>
 * WorkflowException is thrown when errors occur during workflow building,
 * script generation, or execution. It extends {@link CoreException} with
 * the {@link CoreException#WORKFLOW_ERROR} error code.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class WorkflowException extends CoreException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new workflow exception with the specified detail message.
     *
     * @param message the detail message describing the workflow failure
     */
    public WorkflowException(String message) {
        super(WORKFLOW_ERROR, message);
    }

    /**
     * Constructs a new workflow exception with the specified detail message and cause.
     *
     * @param message the detail message describing the workflow failure
     * @param cause   the underlying cause of the failure
     */
    public WorkflowException(String message, Throwable cause) {
        super(WORKFLOW_ERROR, message, cause);
    }

    /**
     * Constructs a new workflow exception wrapping the specified cause.
     *
     * @param cause the underlying cause of the failure
     */
    public WorkflowException(Throwable cause) {
        super(WORKFLOW_ERROR, cause);
    }
}
