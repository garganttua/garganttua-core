package com.garganttua.core.execution;

@FunctionalInterface
public interface IFailBackExecutor<T> {

	void failBack(T request, IExecutorChain<T> nextExecutor);
}
