package com.garganttua.executor.chain;

@FunctionalInterface
public interface IGGExecutor<Type> {
	
	void execute(Type request, IGGExecutorChain<Type> nextExecutor) throws GGExecutorException;

}
