package com.garganttua.di.impl.supplier;

import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IPropertyProvider;

public class DummyPropertyProvider implements IPropertyProvider {

    private String name;

    public DummyPropertyProvider(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
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
    public IPropertyProvider onStart() throws DiException {
        return this;
    }

    @Override
    public IPropertyProvider onStop() throws DiException {
        return this;
    }

    @Override
    public IPropertyProvider onFlush() throws DiException {
        return this;
    }

    @Override
    public IPropertyProvider onInit() throws DiException {
        return this;
    }

    @Override
    public IPropertyProvider onReload() throws DiException {
        return this;
    }

}
