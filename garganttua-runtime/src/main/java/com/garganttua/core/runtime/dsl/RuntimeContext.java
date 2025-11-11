package com.garganttua.core.runtime.dsl;

import java.util.List;
import java.util.Map;

import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.runtime.IRuntimeContext;
import com.garganttua.core.runtime.IRuntimeResult;

public class RuntimeContext<InputType, OutputType> extends DiContext implements IRuntimeContext<InputType, OutputType> {

    private InputType input;
    private Class<OutputType> outputType;

    public RuntimeContext(InputType input, Class<OutputType> outputType, Map<String, IBeanProvider> beanProviders, Map<String, IPropertyProvider> propertyProviders,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories) {
        super(beanProviders, propertyProviders, childContextFactories);
        this.input = input;
        this.outputType = outputType;
        this.initialized.set(true);
        this.started.set( true);
    }

    @Override
    public IRuntimeResult<InputType, OutputType> getResult() {
        return null;
    }

}
