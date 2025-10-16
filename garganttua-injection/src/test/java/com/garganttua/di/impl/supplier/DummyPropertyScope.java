package com.garganttua.di.impl.supplier;

import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.IPropertyScope;

public class DummyPropertyScope implements IPropertyScope {

    private String name;

    public DummyPropertyScope(String name) {
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
    public void onStart() throws DiException {

    }

    @Override
    public void onStop() throws DiException {

    }

    @Override
    public void onFlush() throws DiException {

    }

    @Override
    public void onInit() throws DiException {

    }

    @Override
    public void onReload() throws DiException {

    }

}
