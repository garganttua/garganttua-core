package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

public interface IRuntimeBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>, IDependentBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntime<InputType, OutputType>> {

    IRuntimeBuilder<InputType, OutputType> variable(String name, Object value);

    IRuntimeBuilder<InputType, OutputType> variable(String name, ISupplierBuilder<?, ? extends ISupplier<?>> value);

    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

    <StepObjectType, ExecutionReturn> IRuntimeStepBuilder<ExecutionReturn, StepObjectType, InputType, OutputType> step(String string, OrderedMapPosition<String> position, ISupplierBuilder<StepObjectType, ISupplier<StepObjectType>> objectSupplier, Class<ExecutionReturn> returnType);

}
