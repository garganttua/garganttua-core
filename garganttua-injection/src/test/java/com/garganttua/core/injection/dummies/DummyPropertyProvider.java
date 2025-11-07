package com.garganttua.core.injection.dummies;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.lifecycle.LifecycleException;

public class DummyPropertyProvider implements IPropertyProvider {

    private String name;

    public DummyPropertyProvider(String name) {
        this.name = name;
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        return null;
    }

    @Override
    public void setProperty(String key, Object value) {

    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Set<String> keys() {
        return null;
    }

    @Override
    public IPropertyProvider onStart() throws LifecycleException {
        return this;
    }

    @Override
    public IPropertyProvider onStop() throws LifecycleException {
        return this;
    }

    @Override
    public IPropertyProvider onFlush() throws LifecycleException {
        return this;
    }

    @Override
    public IPropertyProvider onInit() throws LifecycleException {
        return this;
    }

    @Override
    public IPropertyProvider onReload() throws LifecycleException {
        return this;
    }

}
