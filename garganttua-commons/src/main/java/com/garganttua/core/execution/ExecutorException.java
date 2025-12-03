package com.garganttua.core.execution;

import com.garganttua.core.CoreException;

import lombok.extern.slf4j.Slf4j;

/**
 * Exception thrown when an error occurs during executor execution.
 * <p>
 * This exception is used to signal failures in the executor chain processing.
 * It wraps underlying exceptions and provides context about executor-specific errors.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * IExecutor&lt;Request&gt; validator = (request, chain) -&gt; {
 *     if (!request.isValid()) {
 *         throw new ExecutorException("Request validation failed");
 *     }
 *     chain.execute(request);
 * };
 *
 * try {
 *     chain.execute(request);
 * } catch (ExecutorException e) {
 *     System.err.println("Execution failed: " + e.getMessage());
 * }
 * </pre>
 *
 * @since 2.0.0-ALPHA01
 */
@Slf4j
public class ExecutorException extends CoreException {

	private static final long serialVersionUID = 4089999852587836549L;

	/**
	 * Creates a new ExecutorException wrapping an existing exception.
	 *
	 * @param t the underlying exception that caused the executor failure
	 */
	public ExecutorException(Exception t) {
		super(CoreException.EXECUTOR_ERROR, t);
		log.atTrace().log("Exiting ExecutorException constructor");
	}

	/**
	 * Creates a new ExecutorException with a descriptive message.
	 *
	 * @param string the error message describing the executor failure
	 */
	public ExecutorException(String string) {
		super(CoreException.EXECUTOR_ERROR, string);
		log.atTrace().log("Exiting ExecutorException constructor");
	}

	/**
	 * Creates a new ExecutorException with a message and a cause.
	 *
	 * @param string the error message describing the executor failure
	 * @param t the underlying cause of the executor failure
	 */
	public ExecutorException(String string, Throwable t) {
		super(CoreException.EXECUTOR_ERROR, string, t);
		log.atTrace().log("Exiting ExecutorException constructor");
	}
}
