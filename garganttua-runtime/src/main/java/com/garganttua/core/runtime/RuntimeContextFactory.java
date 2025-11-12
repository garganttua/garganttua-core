package com.garganttua.core.runtime;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.DiContext;

public class RuntimeContextFactory implements IDiChildContextFactory<IRuntimeContext<?,?>> {

    @Override
    public IRuntimeContext<?, ?> createChildContext(IDiContext parent, Object... args) throws DiException {
        Object input = args[0];
        Class<?> outputType = (Class<?>) args[1];
        
        return new RuntimeContext<>(input, outputType, ((DiContext) parent).beanProviders(),((DiContext) parent).propertyProviders(), (((DiContext) parent).childContextFactories()));
    }

}
