package com.garganttua.core.executor;

@FunctionalInterface
public interface IExecutor<T> {
	
	void execute(T request, IExecutorChain<T> nextExecutor) throws ExecutorException;

}
