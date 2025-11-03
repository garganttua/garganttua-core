package com.garganttua.injection.spec;

import com.garganttua.dsl.IBuilder;

public interface IDiContextBuilder extends IBuilder<IDiContext> {

    IDiContextBuilder withPackage(String packageName);

    IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider);

    IBeanProviderBuilder beanProvider(String provider);

    IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider);

    IPropertyProviderBuilder propertyProvider(String provider);

    IDiContextBuilder childContextFactory(IDiChildContextFactory<IDiContext> instanciator);

    IDiContextBuilder withPackages(String[] packageNames);

}
