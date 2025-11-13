package com.garganttua.core.execution;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutorChain<T> implements IExecutorChain<T> {

	private Queue<Entry<IExecutor<T>, IFallBackExecutor<T>>> executors;

	private Queue<IFallBackExecutor<T>> fallBackExecutors;

	public ExecutorChain() {
		this.executors = new LinkedList<>();
		this.fallBackExecutors = new LinkedList<>();
	}

	@Override
	public void addExecutor(IExecutor<T> executor) {
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
	}

	@Override
	public void execute(T request) throws ExecutorException {
		Entry<IExecutor<T>, IFallBackExecutor<T>> executor = this.executors.poll();
		if (executor != null) {
			if (executor.getValue() != null) {
				((LinkedList<IFallBackExecutor<T>>) this.fallBackExecutors).addFirst(executor.getValue());
			}
			try {
				executor.getKey().execute(request, this);
			} catch (ExecutorException e) {
				log.atWarn().log("Error during executor chain execution.", e);
				if (!this.fallBackExecutors.isEmpty()) {
					log.atInfo().log("Executing Falling back executors");
					this.executeFallBack(request);
				}
				throw e;
			}
		}
	}

	@Override
	public void executeFallBack(T request) {
		IFallBackExecutor<T> executor = this.fallBackExecutors.poll();
		if (executor != null) {
			executor.fallBack(request, this);
		}
	}

	@Override
	public void addExecutor(IExecutor<T> executor, IFallBackExecutor<T> fallBackExecutor) {
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
	}
}
