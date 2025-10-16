package com.garganttua.di.impl.supplier;

import java.util.List;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanScope;

public class DummyBeanScope implements IBeanScope {

    private String name;

    public DummyBeanScope(String name) {
        this.name = name;
    }

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
    public String getName() {
        return this.name;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getBean(Class<T> type) {
        return (Optional<T>) Optional.of(new DummyBean());
    }

    @Override
    public <T> Optional<T> getBean(String name, Class<T> type) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBean'");
    }

    @Override
    public void registerBean(String name, Object bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'registerBean'");
    }

    @Override
    public boolean isMutable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isMutable'");
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(Class<T> interfasse) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

}
