package com.garganttua.injection.spec;

import com.garganttua.dsl.DslException;
import com.garganttua.dsl.IAutomaticLinkedBuilder;
import com.garganttua.injection.beans.IBeanFactoryBuilder;

public interface IBeanProviderBuilder extends IAutomaticLinkedBuilder<IBeanProviderBuilder, IDiContextBuilder, IBeanProvider> {

    <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException;

    IBeanProviderBuilder withPackage(String packageName);

    IBeanProviderBuilder  withPackages(String[] packageNames);

}
