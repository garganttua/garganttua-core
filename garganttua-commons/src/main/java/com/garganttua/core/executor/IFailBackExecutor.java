package com.garganttua.core.executor;

@FunctionalInterface
public interface IFailBackExecutor<T> {

	void failBack(T request, IExecutorChain<T> nextExecutor);
}
