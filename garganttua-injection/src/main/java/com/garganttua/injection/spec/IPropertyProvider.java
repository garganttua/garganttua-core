package com.garganttua.injection.spec;

import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;

public interface IPropertyProvider extends ILifecycle {

    <T> Optional<T> getProperty(String key, Class<T> type) throws DiException;

    void setProperty(String key, Object value) throws DiException;

    boolean isMutable();

    Set<String> keys();
}
