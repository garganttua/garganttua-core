package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;

public interface IDiContextBuilder extends IBuilder<IDiContext> {

    IDiContextBuilder withPackage(String packageName);

    IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider);

    IBeanProviderBuilder beanProvider(String provider);

    IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider);

    IPropertyProviderBuilder propertyProvider(String provider);

    IDiContextBuilder childContextFactory(IDiChildContextFactory<IDiContext> instanciator);

    IDiContextBuilder withPackages(String[] packageNames);

}
