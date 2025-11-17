package com.garganttua.core.runtime;

import java.util.Map;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.supplying.IObjectSupplier;

public class RuntimeContextFactory implements IDiChildContextFactory<IRuntimeContext<?, ?>> {

    @SuppressWarnings("unchecked")
    @Override
    public IRuntimeContext<?, ?> createChildContext(IDiContext parent, Object... args) throws DiException {
        Object input = args[0];
        Class<?> outputType = (Class<?>) args[1];
        Map<String, IObjectSupplier<?>> presetVariables = (Map<String, IObjectSupplier<?>>) args[2];

        return new RuntimeContext<>(parent, input, outputType, ((DiContext) parent).beanProviders(),
                ((DiContext) parent).propertyProviders(), (((DiContext) parent).childContextFactories()), presetVariables);
    }

}
