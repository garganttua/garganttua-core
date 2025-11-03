package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IAutomaticLinkedBuilder;
import com.garganttua.core.injection.IBeanProvider;

public interface IBeanProviderBuilder extends IAutomaticLinkedBuilder<IBeanProviderBuilder, IDiContextBuilder, IBeanProvider> {

    <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException;

    IBeanProviderBuilder withPackage(String packageName);

    IBeanProviderBuilder  withPackages(String[] packageNames);

}
