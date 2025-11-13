package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

public interface IRuntimeStageBuilder<InputType, OutputType> extends ILinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimeStage> {

    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType> step(String string, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType> step(String string, OrderedMapPosition<String> position, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

}
