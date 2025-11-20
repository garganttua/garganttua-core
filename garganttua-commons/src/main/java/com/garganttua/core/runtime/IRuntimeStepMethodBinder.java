package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.execution.IExecutor;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public interface IRuntimeStepMethodBinder<ExecutionReturned, OwnerContextType, InputType,OutputType> extends IContextualMethodBinder<ExecutionReturned, OwnerContextType>, IExecutor<OwnerContextType> {

    boolean isOutput();

    Optional<String> variable();

    void setCode(IRuntimeContext<?,?> c);

    boolean nullable();

}
