package com.garganttua.core.reflection.binders;

public interface IContextualMethodBinder<ExecutionReturned, OwnerContextType> extends IMethodBinder<ExecutionReturned>, IContextualExecutableBinder<ExecutionReturned, OwnerContextType> {

}
