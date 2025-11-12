package com.garganttua.core.execution;

@FunctionalInterface
public interface IExecutor<T> {
	
	void execute(T request, IExecutorChain<T> nextExecutor) throws ExecutorException;

}
