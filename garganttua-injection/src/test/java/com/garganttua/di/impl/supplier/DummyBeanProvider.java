package com.garganttua.di.impl.supplier;

import java.util.List;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IBeanProvider;

public class DummyBeanProvider implements IBeanProvider {

    private String name;

    public DummyBeanProvider(String name) {
        this.name = name;
    }

    @Override
    public IBeanProvider onStart() throws DiException {
        return this;
    }

    @Override
    public IBeanProvider onStop() throws DiException {
        return this;
    }

    @Override
    public IBeanProvider onFlush() throws DiException {
        return this;
    }

    @Override
    public IBeanProvider onInit() throws DiException {
        return this;
    }

    @Override
    public IBeanProvider onReload() throws DiException {
        return this;
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
    public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

}
