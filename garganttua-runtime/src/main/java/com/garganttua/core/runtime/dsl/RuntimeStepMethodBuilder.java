package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public class RuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> extends
        AbstractMethodBinderBuilder<ExecutionReturn, IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType>, IRuntimeStepBuilder<ExecutionReturn, StepObjectType>>
        implements
        IRuntimeStepMethodBuilder<ExecutionReturn, StepObjectType> {

    protected RuntimeStepMethodBuilder(IRuntimeStepBuilder<ExecutionReturn, StepObjectType> up,
            IObjectSupplierBuilder<StepObjectType, ? extends IObjectSupplier<StepObjectType>> supplier) throws DslException {
        super(up, supplier);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void doAutoDetection() throws DslException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'doAutoDetection'");
    }

}
