package com.garganttua.core.execution;

@FunctionalInterface
public interface IFallBackExecutor<T> {

	void fallBack(T request, IExecutorChain<T> nextExecutor);
}
