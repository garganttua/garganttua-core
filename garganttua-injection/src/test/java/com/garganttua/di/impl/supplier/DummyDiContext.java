package com.garganttua.di.impl.supplier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IPropertyProvider;

public class DummyDiContext implements IDiContext{

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
    public <T> Optional<T> getBean(Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBean'");
    }

    @Override
    public <T> Optional<T> getBean(String name, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBean'");
    }

    @Override
    public <T> Optional<T> getBeanFromProvider(String scopeName, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanFromProvider'");
    }

    @Override
    public <T> Optional<T> getBeanFromProvider(String scopeName, String name, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanFromProvider'");
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(String scopeName, Class<T> interfasse) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

    @Override
    public void setBeanInProvider(String scopeName, String name, Object bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setBeanInProvider'");
    }

    @Override
    public void setBeanInProvider(String scopeName, Object bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setBeanInProvider'");
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
    public <T> Optional<T> getPropertyFromProvider(String scopeName, String key, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyFromProvider'");
    }

    @Override
    public void setPropertyInProvider(String scopeName, String key, Object value) throws DiException {
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
    public <T> List<T> getBeansImplementingInterface(String providerName, Class<T> interfasse,
            boolean includePrototypes) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

}
