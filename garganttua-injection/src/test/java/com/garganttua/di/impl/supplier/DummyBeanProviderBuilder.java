package com.garganttua.di.impl.supplier;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.beans.IBeanFactoryBuilder;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IBeanProviderBuilder;
import com.garganttua.injection.spec.IDiContextBuilder;

public class DummyBeanProviderBuilder implements IBeanProviderBuilder {

    private IDiContextBuilder parentContext;

    @Override
    public IBeanProviderBuilder autoDetect(boolean b) {
        // Ignore setting in dummy implementation
        return this;
    }

    @Override
    public IDiContextBuilder up() {
        if (this.parentContext == null) {
            throw new IllegalStateException("No parent context set for DummyBeanProviderBuilder");
        }
        return this.parentContext;
    }

    @Override
    public IBeanProviderBuilder setUp(IDiContextBuilder link) {
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
