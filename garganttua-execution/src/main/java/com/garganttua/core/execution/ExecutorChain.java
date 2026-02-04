package com.garganttua.core.execution;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutorChain<T> implements IExecutorChain<T> {

	private final Queue<Entry<IExecutor<T>, IFallBackExecutor<T>>> executors;

	private final Queue<IFallBackExecutor<T>> fallBackExecutors;

	private final boolean rethrow;

	public ExecutorChain() {
		log.atTrace().log("Entering default constructor ExecutorChain()");
		this.executors = new LinkedList<>();
		this.fallBackExecutors = new LinkedList<>();
		this.rethrow = true;
		log.atTrace().log("Exiting default constructor ExecutorChain()");
	}

	public ExecutorChain(boolean rethrow) {
		log.atTrace().log("Entering constructor ExecutorChain(boolean rethrow) with rethrow={}", rethrow);
		this.executors = new LinkedList<>();
		this.fallBackExecutors = new LinkedList<>();
		this.rethrow = rethrow;
		log.atTrace().log("Exiting constructor ExecutorChain(boolean rethrow)");
	}

	@Override
	public void addExecutor(IExecutor<T> executor) {
		log.atTrace().log("Entering addExecutor(IExecutor) with executor={}", executor);
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
		log.atDebug().log("Executor added without fallback: {}", executor);
		log.atTrace().log("Exiting addExecutor(IExecutor)");
	}

	@Override
	public void execute(T request) throws ExecutorException {
		log.atTrace().log("Entering execute() with request={}", request);

		// Use iterative approach with trampoline pattern to avoid stack overflow
		// on chains with thousands of executors
		// Use array to hold the current request (allows modification from inner class)
		@SuppressWarnings("unchecked")
		final Object[] currentRequest = new Object[] { request };

		while (true) {
			Entry<IExecutor<T>, IFallBackExecutor<T>> executor = this.executors.poll();
			if (executor == null) {
				log.atDebug().log("No executor available to execute");
				break;
			}

			log.atDebug().log("Executor polled: {}", executor.getKey());
			if (executor.getValue() != null) {
				((LinkedList<IFallBackExecutor<T>>) this.fallBackExecutors).addFirst(executor.getValue());
				log.atDebug().log("Fallback executor added to front of queue: {}", executor.getValue());
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
				log.atDebug().log("Executing executor: {}", executor.getKey());
				@SuppressWarnings("unchecked")
				T reqToExecute = (T) currentRequest[0];
				executor.getKey().execute(reqToExecute, nextProxy);
				log.atDebug().log("Executor executed successfully: {}", executor.getKey());

				if (!shouldContinue.get()) {
					// Executor didn't call next.execute(), stop the chain
					log.atDebug().log("Executor did not continue chain, stopping");
					break;
				}
			} catch (ExecutorException e) {
				log.atWarn().log("Error during executor chain execution for executor: {}", executor.getKey(), e);
				if (!this.fallBackExecutors.isEmpty()) {
					log.atDebug().log("Executing fallback executors");
					@SuppressWarnings("unchecked")
					T reqForFallback = (T) currentRequest[0];
					this.executeFallBack(reqForFallback);
				}
				if (this.rethrow) {
					log.atError().log("Rethrowing ExecutorException due to rethrow=true");
					throw e;
				}
				break; // Stop the chain after exception
			}
		}

		log.atTrace().log("Exiting execute()");
	}

	@Override
	public void executeFallBack(T request) {
		log.atTrace().log("Entering executeFallBack() with request={}", request);
		IFallBackExecutor<T> executor = this.fallBackExecutors.poll();
		if (executor != null) {
			log.atDebug().log("Executing fallback executor: {}", executor);
			executor.fallBack(request, this);
			log.atDebug().log("Fallback executor executed successfully: {}", executor);
		} else {
			log.atDebug().log("No fallback executor available to execute");
		}
		log.atTrace().log("Exiting executeFallBack()");
	}

	@Override
	public void addExecutor(IExecutor<T> executor, IFallBackExecutor<T> fallBackExecutor) {
		log.atTrace().log("Entering addExecutor(IExecutor, IFallBackExecutor) with executor={} and fallback={}",
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
		log.atDebug().log("Executor added with fallback: {} -> {}", executor, fallBackExecutor);
		log.atTrace().log("Exiting addExecutor(IExecutor, IFallBackExecutor)");
	}
}
