package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IRuntimeStage;
import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

public interface IRuntimeStageBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeStageBuilder<InputType, OutputType>, IRuntimeBuilder<InputType, OutputType>, IRuntimeStage<InputType, OutputType> >, IContextBuilderObserver {

    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, OrderedMapPosition<String> position, IObjectSupplierBuilder<StepObjectType, IObjectSupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

}
