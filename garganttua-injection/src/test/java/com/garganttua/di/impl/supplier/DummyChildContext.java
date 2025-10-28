package com.garganttua.di.impl.supplier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanDefinition;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IPropertyProvider;

public class DummyChildContext implements IDiContext{

    @Override
    public IDiContext onStart() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onStart'");
    }

    @Override
    public IDiContext onStop() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onStop'");
    }

    @Override
    public IDiContext onFlush() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onFlush'");
    }

    @Override
    public IDiContext onInit() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onInit'");
    }

    @Override
    public IDiContext onReload() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onReload'");
    }

    @Override
    public Set<IBeanProvider> getBeanProviders() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanProviders'");
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyProviders'");
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProperty'");
    }

    @Override
    public <T> Optional<T> getProperty(String scopeName, String key, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyFromProvider'");
    }

    @Override
    public void setProperty(String scopeName, String key, Object value) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPropertyInProvider'");
    }

    @Override
    public <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newChildContext'");
    }

    @Override
    public <ChildContext extends IDiContext> Set<IDiChildContextFactory<ChildContext>> getChildContextFactories() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getChildContextFactories'");
    }

    @Override
    public <Bean> Optional<Bean> queryBean(Optional<String> ofNullable, BeanDefinition<Bean> definition)
            throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanDefinition<Bean> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanDefinition<Bean> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProperty'");
    }

}
