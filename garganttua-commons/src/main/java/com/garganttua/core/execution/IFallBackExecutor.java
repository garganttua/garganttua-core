package com.garganttua.core.execution;

/**
 * Represents a fallback executor that handles errors in an executor chain.
 * <p>
 * This functional interface defines a fallback mechanism that is invoked when
 * an executor in the chain throws an exception. Fallback executors can perform
 * error recovery, alternative processing, logging, or graceful degradation.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * // Create a fallback executor for handling database failures
 * IFallBackExecutor&lt;DataRequest&gt; cacheFallback = (request, chain) -&gt; {
 *     System.out.println("Database failed, using cache");
 *     request.loadFromCache();
 *     chain.execute(request); // Continue with cached data
 * };
 *
 * // Create a main executor that might fail
 * IExecutor&lt;DataRequest&gt; dbExecutor = (request, chain) -&gt; {
 *     if (!database.isAvailable()) {
 *         throw new ExecutorException("Database unavailable");
 *     }
 *     request.loadFromDatabase();
 *     chain.execute(request);
 * };
 *
 * // Add both to the chain
 * IExecutorChain&lt;DataRequest&gt; chain = new ExecutorChain&lt;&gt;();
 * chain.addExecutor(dbExecutor, cacheFallback);
 * chain.execute(myRequest); // Falls back to cache if database fails
 * </pre>
 *
 * @param <T> the type of request this fallback executor handles
 *
 * @since 2.0.0-ALPHA01
 */
@FunctionalInterface
public interface IFallBackExecutor<T> {

	/**
	 * Executes the fallback logic for a failed executor.
	 * <p>
	 * This method is invoked when the associated executor throws an exception.
	 * Implementations can perform error recovery, alternative processing, or
	 * graceful degradation. After handling the error, the fallback can optionally
	 * continue the chain by calling the next executor.
	 * </p>
	 *
	 * @param request the request that was being processed when the error occurred
	 * @param nextExecutor the next executor in the chain to delegate to
	 */
	void fallBack(T request, IExecutorChain<T> nextExecutor);
}
