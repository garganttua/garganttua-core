package com.garganttua.di.impl.supplier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanScope;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IPropertyScope;

public class DummyDiContext implements IDiContext{

    @Override
    public void onStart() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onStart'");
    }

    @Override
    public void onStop() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onStop'");
    }

    @Override
    public void onFlush() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onFlush'");
    }

    @Override
    public void onInit() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onInit'");
    }

    @Override
    public void onReload() throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onReload'");
    }

    @Override
    public Set<IBeanScope> getBeanScopes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanScopes'");
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
    public <T> Optional<T> getBeanFromScope(String scopeName, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanFromScope'");
    }

    @Override
    public <T> Optional<T> getBeanFromScope(String scopeName, String name, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeanFromScope'");
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(String scopeName, Class<T> interfasse) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

    @Override
    public void setBeanInScope(String scopeName, String name, Object bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setBeanInScope'");
    }

    @Override
    public void setBeanInScope(String scopeName, Object bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setBeanInScope'");
    }

    @Override
    public Set<IPropertyScope> getPropertyScopes() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyScopes'");
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getProperty'");
    }

    @Override
    public <T> Optional<T> getPropertyFromScope(String scopeName, String key, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyFromScope'");
    }

    @Override
    public void setPropertyInScope(String scopeName, String key, Object value) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setPropertyInScope'");
    }

    @Override
    public void doInjection(Object instance) throws DiException {
        
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

}
