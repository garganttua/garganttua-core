package com.garganttua.core.runtime;

import java.util.Optional;

import com.garganttua.core.execution.IFallBackExecutor;
import com.garganttua.core.reflection.binders.IContextualMethodBinder;

public interface IRuntimeStepFallbackBinder<ExecutionReturned, OwnerContextType, InputType,OutputType> extends IContextualMethodBinder<ExecutionReturned, OwnerContextType>, IFallBackExecutor<OwnerContextType> {

    boolean isOutput();

    Optional<String> variable();

    boolean nullable();

}
