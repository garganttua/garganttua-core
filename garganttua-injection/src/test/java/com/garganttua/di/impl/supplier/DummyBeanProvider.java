package com.garganttua.di.impl.supplier;

import java.util.List;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanDefinition;
import com.garganttua.injection.spec.IBeanProvider;

public class DummyBeanProvider implements IBeanProvider {


    public DummyBeanProvider() {

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
    public boolean isMutable() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isMutable'");
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

    @Override
    public <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

}
