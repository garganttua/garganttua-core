package com.garganttua.executor.chain;

public interface IGGExecutorChain<Type> {

	void execute(Type request) throws GGExecutorException;

	void addExecutor(IGGExecutor<Type> executor);

}
