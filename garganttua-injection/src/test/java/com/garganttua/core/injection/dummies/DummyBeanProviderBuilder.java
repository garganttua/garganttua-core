package com.garganttua.core.injection.dummies;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.context.dsl.IBeanFactoryBuilder;
import com.garganttua.core.injection.context.dsl.IBeanProviderBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;

public class DummyBeanProviderBuilder implements IBeanProviderBuilder {

    private IInjectionContextBuilder parentContext;

    @Override
    public IBeanProviderBuilder autoDetect(boolean b) {
        // Ignore setting in dummy implementation
        return this;
    }

    @Override
    public IInjectionContextBuilder up() {
        if (this.parentContext == null) {
            throw new IllegalStateException("No parent context set for DummyBeanProviderBuilder");
        }
        return this.parentContext;
    }

    @Override
    public IBeanProviderBuilder setUp(IInjectionContextBuilder link) {
        this.parentContext = link;
        return this;
    }

    @Override
    public IBeanProvider build() {
        return new DummyBeanProvider();
    }

    @Override
    public <BeanType> IBeanFactoryBuilder<BeanType> withBean(Class<BeanType> beanType) throws DslException {
        throw new UnsupportedOperationException("DummyBeanProviderBuilder does not support bean registration");
    }

    @Override
    public IBeanProviderBuilder withPackage(String packageName) {
        // Ignore in dummy impl
        return this;
    }

    @Override
    public IBeanProviderBuilder withPackages(String[] packageNames) {
        // Ignore in dummy impl
        return this;
    }

}
