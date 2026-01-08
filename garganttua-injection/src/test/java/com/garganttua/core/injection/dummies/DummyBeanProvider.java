package com.garganttua.core.injection.dummies;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.lifecycle.LifecycleStatus;
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
    public <T> Optional<T> get(Class<T> type) {
        return (Optional<T>) Optional.of(new DummyBean());
    }

    @Override
    public <T> Optional<T> get(String name, Class<T> type) {
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }

    @Override
    public boolean isMutable() {
        throw new UnsupportedOperationException("Unimplemented method 'isMutable'");
    }

    @Override
    public <T> List<T> get(Class<T> interfasse, boolean includePrototypes) {
        throw new UnsupportedOperationException("Unimplemented method 'get'");
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

    @Override
    public <T> Optional<T> query(BeanReference<T> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'query'");
    }

    @Override
    public <T> List<T> queries(BeanReference<T> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queries'");
    }

    @Override
    public <T> void add(BeanReference<T> query, T bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public <T> void add(BeanReference<T> reference, T bean, boolean autoDetect) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public <T> void add(BeanReference<T> reference, Optional<T> bean, boolean autoDetect) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public <T> void add(BeanReference<T> reference, Optional<T> bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public <T> void add(BeanReference<T> reference) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public <T> void add(BeanReference<T> reference, boolean autoDetect) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'add'");
    }

    @Override
    public LifecycleStatus status() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'status'");
    }


}
