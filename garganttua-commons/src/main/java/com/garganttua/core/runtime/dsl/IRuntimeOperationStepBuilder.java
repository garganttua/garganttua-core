package com.garganttua.core.runtime.dsl;

import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;

public interface IRuntimeOperationStepBuilder<ExecutionReturn>
        extends IMethodBinderBuilder<ExecutionReturn, IRuntimeOperationStepBuilder<ExecutionReturn>, IRuntimeStepBuilder, IMethodBinder<ExecutionReturn>>{

    IRuntimeOperationStepBuilder<ExecutionReturn> storeReturn(String variableName);

}
