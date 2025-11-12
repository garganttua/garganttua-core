package com.garganttua.core.execution;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutorChain<T> implements IExecutorChain<T> {

	private Queue<Entry<IExecutor<T>, IFailBackExecutor<T>>> executors;

	private Queue<IFailBackExecutor<T>> failBackExecutors;

	public ExecutorChain() {
		this.executors = new LinkedList<>();
		this.failBackExecutors = new LinkedList<>();
	}

	@Override
	public void addExecutor(IExecutor<T> executor) {
		this.executors.add(new Entry<IExecutor<T>, IFailBackExecutor<T>>() {
			@Override
			public IExecutor<T> getKey() {
				return executor;
			}

			@Override
			public IFailBackExecutor<T> getValue() {
				return null;
			}

			@Override
			public IFailBackExecutor<T> setValue(IFailBackExecutor<T> arg0) {
				return arg0;
			}
		});
	}

	@Override
	public void execute(T request) throws ExecutorException {
		Entry<IExecutor<T>, IFailBackExecutor<T>> executor = this.executors.poll();
		if (executor != null) {
			if (executor.getValue() != null) {
				((LinkedList<IFailBackExecutor<T>>) this.failBackExecutors).addFirst(executor.getValue());
			}
			try {
				executor.getKey().execute(request, this);
			} catch (ExecutorException e) {
				log.atWarn().log("Error during executor chain execution.", e);
				if (!this.failBackExecutors.isEmpty()) {
					log.atInfo().log("Executing failing back executors");
					this.executeFailBack(request);
				}
				throw e;
			}
		}
	}

	@Override
	public void executeFailBack(T request) {
		IFailBackExecutor<T> executor = this.failBackExecutors.poll();
		if (executor != null) {
			executor.failBack(request, this);
		}
	}

	@Override
	public void addExecutor(IExecutor<T> executor, IFailBackExecutor<T> failBackExecutor) {
		this.executors.add(new Entry<IExecutor<T>, IFailBackExecutor<T>>() {
			@Override
			public IExecutor<T> getKey() {
				return executor;
			}

			@Override
			public IFailBackExecutor<T> getValue() {
				return failBackExecutor;
			}

			@Override
			public IFailBackExecutor<T> setValue(IFailBackExecutor<T> arg0) {
				return arg0;
			}
		});
	}
}
