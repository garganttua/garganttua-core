package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;

public interface IDiContextBuilder extends IAutomaticBuilder<IDiContextBuilder, IDiContext> {

    IDiContextBuilder withPackage(String packageName);

    IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider);

    IBeanProviderBuilder beanProvider(String provider);

    IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider);

    IPropertyProviderBuilder propertyProvider(String provider);

    IDiContextBuilder childContextFactory(IDiChildContextFactory<? extends IDiContext> factory);

    IDiContextBuilder withPackages(String[] packageNames);

    IInjectableElementResolverBuilder resolvers();

    IDiContextBuilder withQualifier(Class<? extends Annotation> qualifier);

    IDiContextBuilder observer(IContextBuilderObserver observer);

    String[] getPackages();

}
