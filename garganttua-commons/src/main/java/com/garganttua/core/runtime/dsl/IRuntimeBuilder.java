package com.garganttua.core.runtime.dsl;

import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.runtime.IRuntime;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.utils.OrderedMapPosition;

public interface IRuntimeBuilder<InputType, OutputType> extends IAutomaticLinkedBuilder<IRuntimeBuilder<InputType, OutputType>, IRuntimesBuilder, IRuntime<InputType, OutputType>>, IContextBuilderObserver {

    IRuntimeBuilder<InputType, OutputType> variable(String name, Object value);

    IRuntimeBuilder<InputType, OutputType> variable(String name, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> value);

    IRuntimeStageBuilder<InputType, OutputType> stage(String string);

    IRuntimeStageBuilder<InputType, OutputType> stage(String string, OrderedMapPosition<String> position);

}
