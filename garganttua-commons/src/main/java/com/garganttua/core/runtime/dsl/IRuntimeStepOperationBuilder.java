package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;

public interface IRuntimeStepOperationBuilder<ExecutionReturn>
        extends IMethodBinderBuilder<ExecutionReturn, IRuntimeStepOperationBuilder<ExecutionReturn>, IRuntimeStepBuilder, IMethodBinder<ExecutionReturn>>{

    IRuntimeStepOperationBuilder<ExecutionReturn> variable(String variableName);

    IRuntimeStepOperationBuilder<ExecutionReturn> output(boolean output);

    IRuntimeStepCatchBuilder katch(Class<? extends Throwable> exception) throws DslException;

}
