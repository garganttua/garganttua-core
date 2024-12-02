package com.garganttua.executor.chain;

import java.util.LinkedList;
import java.util.Queue;

public class GGExecutorChain<Type> implements IGGExecutorChain<Type> {

	private Queue<IGGExecutor<Type>> executors;

	public GGExecutorChain() {
		this.executors = new LinkedList<IGGExecutor<Type>>();
	}
	
	@Override
	public void addExecutor(IGGExecutor<Type> executor) {
		this.executors.add(executor);
	}

	@Override
	public void execute(Type request) throws GGExecutorException {	
		IGGExecutor<Type> executor = this.executors.poll();
		if( executor != null )
			executor.execute(request, this);
	}

}
