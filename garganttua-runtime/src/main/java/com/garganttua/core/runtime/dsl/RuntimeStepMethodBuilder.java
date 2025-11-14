package com.garganttua.core.runtime.dsl;

import java.util.Arrays;
import java.util.Objects;

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
    }

    @Override
    protected void doAutoDetection() throws DslException {
        
    }

    @Override
    public boolean isThrown(Class<? extends Throwable> exception) {
        Objects.requireNonNull(exception, "Exception cannot be null");
        return Arrays.stream(this.findMethod().getExceptionTypes()).anyMatch(e -> e.isAssignableFrom(exception));
    }

}
