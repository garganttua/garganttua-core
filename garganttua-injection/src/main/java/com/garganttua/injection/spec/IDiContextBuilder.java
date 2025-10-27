package com.garganttua.injection.spec;

import com.garganttua.dsl.IBuilder;

public interface IDiContextBuilder extends IBuilder<IDiContext> {

    IDiContextBuilder beanProvider(IBeanProvider provider);

    IDiContextBuilder propertyProvider(IPropertyProvider provider);

    IDiContextBuilder childContextFactory(IDiChildContextFactory<? extends IDiContext> instanciator);

}
