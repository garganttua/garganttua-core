package com.garganttua.core.injection.dummies;

import java.util.List;
import java.util.Optional;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.utils.CopyException;

public class DummyBeanProvider implements IBeanProvider {


    public DummyBeanProvider() {

    }

    @Override
    public IBeanProvider onStart() throws LifecycleException {
        return this;
    }

    @Override
    public IBeanProvider onStop() throws LifecycleException {
        return this;
    }

    @Override
    public IBeanProvider onFlush() throws LifecycleException {
        return this;
    }

    @Override
    public IBeanProvider onInit() throws LifecycleException {
        return this;
    }

    @Override
    public IBeanProvider onReload() throws LifecycleException {
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

    @Override
    public <T> List<T> queryBeans(BeanDefinition<T> definition) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public IBeanProvider copy() throws CopyException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

}
