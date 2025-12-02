package com.garganttua.core.execution;

/**
 * Represents a unit of execution in a chain of responsibility pattern.
 * <p>
 * This functional interface defines an executor that processes a request and
 * optionally delegates to the next executor in the chain. Executors can be
 * chained together to create complex processing pipelines where each executor
 * handles a specific aspect of the request.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Create a validation executor
 * IExecutor&lt;Request&gt; validator = (request, chain) -&gt; {
 *     if (request.isValid()) {
 *         chain.execute(request); // Continue to next executor
 *     } else {
 *         throw new ExecutorException("Invalid request");
 *     }
 * };
 *
 * // Create a logging executor
 * IExecutor&lt;Request&gt; logger = (request, chain) -&gt; {
 *     System.out.println("Processing: " + request);
 *     chain.execute(request);
 * };
 *
 * // Chain executors together
 * IExecutorChain&lt;Request&gt; chain = new ExecutorChain&lt;&gt;();
 * chain.addExecutor(validator);
 * chain.addExecutor(logger);
 * chain.execute(myRequest);
 * </pre>
 *
 * @param <T> the type of request this executor processes
 *
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IExecutor<T> {

	/**
	 * Executes the processing logic for the given request.
	 * <p>
	 * Implementations should perform their specific processing and optionally
	 * call the next executor in the chain to continue the processing pipeline.
	 * If an executor does not call the chain, the processing stops at that point.
	 * </p>
	 *
	 * @param request the request object to process
	 * @param nextExecutor the next executor in the chain to delegate to
	 * @throws ExecutorException if an error occurs during execution
	 */
	void execute(T request, IExecutorChain<T> nextExecutor) throws ExecutorException;

}
