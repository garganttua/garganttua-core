package com.garganttua.core.injection.dummies;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.lifecycle.LifecycleException;

public class DummyChildContext implements IDiContext{

    @Override
    public IDiContext onStart() throws LifecycleException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onStart'");
    }

    @Override
    public IDiContext onStop() throws LifecycleException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onStop'");
    }

    @Override
    public IDiContext onFlush() throws LifecycleException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onFlush'");
    }

    @Override
    public IDiContext onInit() throws LifecycleException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onInit'");
    }

    @Override
    public IDiContext onReload() throws LifecycleException {
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

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanProvider'");
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyProvider'");
    }

    @Override
    public void registerChildContextFactory(IDiChildContextFactory<? extends IDiContext> factory) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerChildContextFactory'");
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanDefinition<Bean> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanDefinition<Bean> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

}
