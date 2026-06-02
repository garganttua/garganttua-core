package com.garganttua.core.execution;

import com.garganttua.core.observability.Logger;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default {@link IExecutorChain} implementation that runs a queue of {@link IExecutor}s
 * sequentially, passing the current request from one executor to the next.
 *
 * <p>
 * Each executor receives the current request and a chain proxy; calling
 * {@code chain.execute(...)} on that proxy advances to the next executor with a possibly
 * mutated request. An executor that does not call the proxy stops the chain. Execution is
 * driven by an iterative trampoline rather than recursion, so chains of thousands of
 * executors do not overflow the stack.
 * </p>
 *
 * <p>
 * Executors may be registered with an optional {@link IFallBackExecutor}; when an executor
 * throws an {@link ExecutorException}, registered fallbacks are run in reverse registration
 * order. If {@code rethrow} is enabled the exception is propagated after fallbacks complete,
 * otherwise the chain simply stops.
 * </p>
 *
 * @param <T> the type of request flowing through the chain
 */
public class ExecutorChain<T> implements IExecutorChain<T> {
    private static final Logger log = Logger.getLogger(ExecutorChain.class);

	private final Queue<Entry<IExecutor<T>, IFallBackExecutor<T>>> executors;

	private final Queue<IFallBackExecutor<T>> fallBackExecutors;

	private final boolean rethrow;

	/**
	 * Creates a chain that rethrows any {@link ExecutorException} after running fallbacks.
	 */
	public ExecutorChain() {
		log.trace("Entering default constructor ExecutorChain()");
		this.executors = new LinkedList<>();
		this.fallBackExecutors = new LinkedList<>();
		this.rethrow = true;
		log.trace("Exiting default constructor ExecutorChain()");
	}

	/**
	 * Creates a chain with explicit control over exception propagation.
	 *
	 * @param rethrow if {@code true}, an {@link ExecutorException} is rethrown after
	 *                fallbacks run; if {@code false}, the chain stops silently
	 */
	public ExecutorChain(boolean rethrow) {
		log.trace("Entering constructor ExecutorChain(boolean rethrow) with rethrow={}", rethrow);
		this.executors = new LinkedList<>();
		this.fallBackExecutors = new LinkedList<>();
		this.rethrow = rethrow;
		log.trace("Exiting constructor ExecutorChain(boolean rethrow)");
	}

	/**
	 * Appends an executor with no associated fallback to the end of the chain.
	 *
	 * @param executor the executor to add
	 */
	@Override
	public void addExecutor(IExecutor<T> executor) {
		log.trace("Entering addExecutor(IExecutor) with executor={}", executor);
		this.executors.add(new Entry<IExecutor<T>, IFallBackExecutor<T>>() {
			@Override
			public IExecutor<T> getKey() {
				return executor;
			}

			@Override
			public IFallBackExecutor<T> getValue() {
				return null;
			}

			@Override
			public IFallBackExecutor<T> setValue(IFallBackExecutor<T> arg0) {
				return arg0;
			}
		});
		log.debug("Executor added without fallback: {}", executor);
		log.trace("Exiting addExecutor(IExecutor)");
	}

	/**
	 * Runs the chain starting with the given request, advancing through executors until one
	 * stops the chain (does not call the proxy) or the queue is drained.
	 *
	 * @param request the initial request to feed to the first executor
	 * @throws ExecutorException if an executor fails and this chain was built with rethrow enabled
	 */
	@Override
	public void execute(T request) throws ExecutorException {
		log.trace("Entering execute() with request={}", request);

		// Use iterative approach with trampoline pattern to avoid stack overflow
		// on chains with thousands of executors
		// Use array to hold the current request (allows modification from inner class)
		final Object[] currentRequest = new Object[] { request };

		while (true) {
			Entry<IExecutor<T>, IFallBackExecutor<T>> executor = this.executors.poll();
			if (executor == null) {
				log.debug("No executor available to execute");
				break;
			}

			log.debug("Executor polled: {}", executor.getKey());
			if (executor.getValue() != null) {
				((LinkedList<IFallBackExecutor<T>>) this.fallBackExecutors).addFirst(executor.getValue());
				log.debug("Fallback executor added to front of queue: {}", executor.getValue());
			}

			// Flag to track if the executor wants to continue to the next executor
			final AtomicBoolean shouldContinue = new AtomicBoolean(false);

			// Create a proxy chain that captures the next request value instead of recursing
			final ExecutorChain<T> self = this;
			IExecutorChain<T> nextProxy = new IExecutorChain<T>() {
				@Override
				public void execute(T req) throws ExecutorException {
					// Mark that we should continue and capture the new request value
					shouldContinue.set(true);
					currentRequest[0] = req;
				}

				@Override
				public void addExecutor(IExecutor<T> exec) {
					self.addExecutor(exec);
				}

				@Override
				public void addExecutor(IExecutor<T> exec, IFallBackExecutor<T> fallback) {
					self.addExecutor(exec, fallback);
				}

				@Override
				public void executeFallBack(T req) {
					self.executeFallBack(req);
				}
			};

			try {
				log.debug("Executing executor: {}", executor.getKey());
				T reqToExecute = (T) currentRequest[0];
				executor.getKey().execute(reqToExecute, nextProxy);
				log.debug("Executor executed successfully: {}", executor.getKey());

				if (!shouldContinue.get()) {
					// Executor didn't call next.execute(), stop the chain
					log.debug("Executor did not continue chain, stopping");
					break;
				}
			} catch (ExecutorException e) {
				log.warn("Error during executor chain execution for executor: {}", executor.getKey(), e);
				if (!this.fallBackExecutors.isEmpty()) {
					log.debug("Executing fallback executors");
					T reqForFallback = (T) currentRequest[0];
					this.executeFallBack(reqForFallback);
				}
				if (this.rethrow) {
					log.debug("Rethrowing ExecutorException due to rethrow=true");
					throw e;
				}
				break; // Stop the chain after exception
			}
		}

		log.trace("Exiting execute()");
	}

	/**
	 * Runs the next pending fallback executor, if any, against the given request.
	 *
	 * @param request the request to pass to the fallback executor
	 */
	@Override
	public void executeFallBack(T request) {
		log.trace("Entering executeFallBack() with request={}", request);
		IFallBackExecutor<T> executor = this.fallBackExecutors.poll();
		if (executor != null) {
			log.debug("Executing fallback executor: {}", executor);
			executor.fallBack(request, this);
			log.debug("Fallback executor executed successfully: {}", executor);
		} else {
			log.debug("No fallback executor available to execute");
		}
		log.trace("Exiting executeFallBack()");
	}

	/**
	 * Appends an executor together with the fallback to run if it (or a later executor) fails.
	 *
	 * @param executor         the executor to add
	 * @param fallBackExecutor the fallback invoked on {@link ExecutorException}
	 */
	@Override
	public void addExecutor(IExecutor<T> executor, IFallBackExecutor<T> fallBackExecutor) {
		log.trace("Entering addExecutor(IExecutor, IFallBackExecutor) with executor={} and fallback={}",
				executor, fallBackExecutor);
		this.executors.add(new Entry<IExecutor<T>, IFallBackExecutor<T>>() {
			@Override
			public IExecutor<T> getKey() {
				return executor;
			}

			@Override
			public IFallBackExecutor<T> getValue() {
				return fallBackExecutor;
			}

			@Override
			public IFallBackExecutor<T> setValue(IFallBackExecutor<T> arg0) {
				return arg0;
			}
		});
		log.debug("Executor added with fallback: {} -> {}", executor, fallBackExecutor);
		log.trace("Exiting addExecutor(IExecutor, IFallBackExecutor)");
	}
}
