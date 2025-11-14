package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> extends
        AbstractMethodBinderBuilder<ExecutionReturn, IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType>>
        implements
        IRuntimeStepFallbackBuilder<ExecutionReturn, StepObjectType> {

    protected RuntimeStepFallbackBuilder(IRuntimeStepBuilder<ExecutionReturn, StepObjectType> up,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier) throws DslException {
        super(up, supplier);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        
    }

}
