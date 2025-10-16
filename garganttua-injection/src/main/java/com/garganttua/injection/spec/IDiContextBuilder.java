package com.garganttua.injection.spec;

import com.garganttua.dsl.IBuilder;

public interface IDiContextBuilder extends IBuilder<IDiContext> {

    IDiContextBuilder beanScope(IBeanScope scope);

    IDiContextBuilder propertyScope(IPropertyScope scope);

    IDiContextBuilder childContextFactory(IDiChildContextFactory<? extends IDiContext> instanciator);

}
