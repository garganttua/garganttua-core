package com.garganttua.core.runtime;

import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public interface IRuntimeStepMethodBinder<ExecutionReturned, OwnerContextType> extends IContextualMethodBinder<ExecutionReturned, OwnerContextType> {

    boolean isOutput();

    void setSuccessCode(IRuntimeContext<?,?> c);

}
