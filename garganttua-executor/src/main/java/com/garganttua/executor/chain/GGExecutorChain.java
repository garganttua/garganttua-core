package com.garganttua.executor.chain;

import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGExecutorChain<Type> implements IGGExecutorChain<Type> {

	private Queue<Entry<IGGExecutor<Type>, IGGFailBackExecutor<Type>>> executors;

	private Queue<IGGFailBackExecutor<Type>> failBackExecutors;

	public GGExecutorChain() {
		this.executors = new LinkedList<Entry<IGGExecutor<Type>, IGGFailBackExecutor<Type>>>();
		this.failBackExecutors = new LinkedList<IGGFailBackExecutor<Type>>();
	}

	@Override
	public void addExecutor(IGGExecutor<Type> executor) {
		this.executors.add(new Entry<IGGExecutor<Type>, IGGFailBackExecutor<Type>>() {
			@Override
			public IGGExecutor<Type> getKey() {
				return executor;
			}

			@Override
			public IGGFailBackExecutor<Type> getValue() {
				return null;
			}

			@Override
			public IGGFailBackExecutor<Type> setValue(IGGFailBackExecutor<Type> arg0) {
				return arg0;
			}
		});
	}

	@Override
	public void execute(Type request) throws GGExecutorException {
		Entry<IGGExecutor<Type>, IGGFailBackExecutor<Type>> executor = this.executors.poll();
		if (executor != null) {
			if (executor.getValue() != null) {
				((LinkedList<IGGFailBackExecutor<Type>>) this.failBackExecutors).addFirst(executor.getValue());
			}
			try {
				executor.getKey().execute(request, this);
			} catch (GGExecutorException e) {
				log.atWarn().log("Error during executor chain execution.", e);
				if (this.failBackExecutors.size() > 0) {
					log.atInfo().log("Executing failing back executors");
					this.executeFailBack(request);
				}
				throw e;
			}
		}
	}

	@Override
	public void executeFailBack(Type request) {
		IGGFailBackExecutor<Type> executor = this.failBackExecutors.poll();
		if (executor != null) {
			executor.failBack(request, this);
		}
	}

	@Override
	public void addExecutor(IGGExecutor<Type> executor, IGGFailBackExecutor<Type> failBackExecutor) {
		this.executors.add(new Entry<IGGExecutor<Type>, IGGFailBackExecutor<Type>>() {
			@Override
			public IGGExecutor<Type> getKey() {
				return executor;
			}

			@Override
			public IGGFailBackExecutor<Type> getValue() {
				return failBackExecutor;
			}

			@Override
			public IGGFailBackExecutor<Type> setValue(IGGFailBackExecutor<Type> arg0) {
				return arg0;
			}
		});
	}
}
