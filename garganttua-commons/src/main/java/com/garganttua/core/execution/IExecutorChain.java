package com.garganttua.core.execution;

public interface IExecutorChain<T> {

	void execute(T request) throws ExecutorException;

	void addExecutor(IExecutor<T> executor);

	void addExecutor(IExecutor<T> executor, IFailBackExecutor<T> failBackExecutor);

	void executeFailBack(T request);

}
