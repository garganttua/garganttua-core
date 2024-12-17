package com.garganttua.executor.chain;

@FunctionalInterface
public interface IGGFailBackExecutor<Type> {
  
  void failBack(Type request, IGGExecutorChain<Type> nextExecutor);

}
