package com.garganttua.core.runtime.dsl;

import java.util.Map;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.dsl.IContextBuilderObserver;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.runtime.IRuntime;

public interface IRuntimesBuilder
        extends IAutomaticBuilder<IRuntimesBuilder, Map<String, IRuntime<?,?>>>, IContextBuilderObserver, IDiChildContextFactory<IDiContext> {

    <InputType, OutputType> IRuntimeBuilder<InputType, OutputType> runtime(String string, Class<InputType> inputType,
            Class<OutputType> outputType);

    IRuntimesBuilder context(IDiContextBuilder context);

}
