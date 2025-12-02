package com.garganttua.core.injection.dummies;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
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
        throw new UnsupportedOperationException("Unimplemented method 'getBean'");
    }

    @Override
    public boolean isMutable() {
        throw new UnsupportedOperationException("Unimplemented method 'isMutable'");
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes) {
        throw new UnsupportedOperationException("Unimplemented method 'getBeansImplementingInterface'");
    }

    @Override
    public <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <T> List<T> queryBeans(BeanDefinition<T> definition) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public IBeanProvider copy() throws CopyException {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }

    @Override
    public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
        throw new UnsupportedOperationException("Unimplemented method 'nativeConfiguration'");
    }


}
