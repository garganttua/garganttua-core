package com.garganttua.core.execution;

/**
 * Manages a chain of executors following the Chain of Responsibility pattern.
 * <p>
 * This interface provides a mechanism to build and execute a sequence of executors
 * where each executor can perform operations on a request and optionally pass it
 * to the next executor in the chain. It also supports fallback executors that are
 * invoked when an executor throws an exception.
 * </p>
 *
 * <h2>Features:</h2>
 * <ul>
 *   <li>Sequential execution of multiple executors</li>
 *   <li>Fallback mechanism for error handling</li>
 *   <li>Flexible chain construction</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * IExecutorChain&lt;HttpRequest&gt; chain = new ExecutorChain&lt;&gt;();
 *
 * // Add authentication executor with fallback
 * IExecutor&lt;HttpRequest&gt; auth = (request, next) -&gt; {
 *     if (request.hasValidToken()) {
 *         next.execute(request);
 *     } else {
 *         throw new ExecutorException("Unauthorized");
 *     }
 * };
 *
 * IFallBackExecutor&lt;HttpRequest&gt; authFallback = (request, next) -&gt; {
 *     System.err.println("Authentication failed");
 *     // Handle authentication failure
 * };
 *
 * chain.addExecutor(auth, authFallback);
 *
 * // Add processing executor without fallback
 * IExecutor&lt;HttpRequest&gt; processor = (request, next) -&gt; {
 *     request.process();
 *     next.execute(request);
 * };
 *
 * chain.addExecutor(processor);
 *
 * // Execute the chain
 * try {
 *     chain.execute(myRequest);
 * } catch (ExecutorException e) {
 *     // Handle unrecovered errors
 * }
 * </pre>
 *
 * @param <T> the type of request processed by this chain
 *
 * @since 2.0.0-ALPHA01
 */
public interface IExecutorChain<T> {

	/**
	 * Executes the chain of executors with the given request.
	 * <p>
	 * The request is passed through each executor in the order they were added.
	 * If an executor throws an exception and has a fallback executor configured,
	 * the fallback is invoked. Otherwise, the exception propagates to the caller.
	 * </p>
	 *
	 * @param request the request to process through the executor chain
	 * @throws ExecutorException if an error occurs during execution and no fallback handles it
	 */
	void execute(T request) throws ExecutorException;

	/**
	 * Adds an executor to the end of the chain without a fallback mechanism.
	 *
	 * @param executor the executor to add to the chain
	 */
	void addExecutor(IExecutor<T> executor);

	/**
	 * Adds an executor to the end of the chain with a fallback mechanism.
	 * <p>
	 * If the executor throws an exception during execution, the fallback executor
	 * is invoked to handle the error. The fallback can attempt recovery or
	 * alternative processing paths.
	 * </p>
	 *
	 * @param executor the executor to add to the chain
	 * @param fallBackExecutor the fallback executor to invoke if the main executor fails
	 */
	void addExecutor(IExecutor<T> executor, IFallBackExecutor<T> fallBackExecutor);

	/**
	 * Manually triggers the fallback execution for the given request.
	 * <p>
	 * This method allows explicit invocation of the fallback chain, which can be
	 * useful for error recovery scenarios or alternative processing paths.
	 * </p>
	 *
	 * @param request the request to process through the fallback chain
	 */
	void executeFallBack(T request);

}
