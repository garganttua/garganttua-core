package com.garganttua.injection.spec;

import java.util.Optional;
import java.util.Set;

public interface IPropertyScope extends ILifecycle {
    String getName();

    <T> Optional<T> getProperty(String key, Class<T> type);

    void setProperty(String key, Object value);

    boolean isMutable();

    Set<String> keys();
}
