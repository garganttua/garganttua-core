package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStep;
import com.garganttua.core.runtime.RuntimeStepOperationPosition;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;

public interface IRuntimeStepBuilder extends ILinkedBuilder<IRuntimeStageBuilder, IRuntimeStep> {

        <T, ExecutionReturn> IRuntimeStepOperationBuilder<ExecutionReturn> object(
                        IObjectSupplierBuilder<T, IObjectSupplier<T>> objectSupplier, Class<ExecutionReturn> returnType)
                        throws DslException;

        <T, ExecutionReturn> IRuntimeStepOperationBuilder<ExecutionReturn> object(
                        IObjectSupplierBuilder<T, IObjectSupplier<T>> objectSupplier, Class<ExecutionReturn> returnType,
                        RuntimeStepOperationPosition position) throws DslException;

}
