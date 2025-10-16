package com.garganttua.injection.spec;

import java.util.List;
import java.util.Optional;

import com.garganttua.injection.DiException;

public interface IBeanScope extends ILifecycle {

    String getName();

    <T> Optional<T> getBean(Class<T> type);

    <T> Optional<T> getBean(String name, Class<T> type);

    <T> List<T> getBeansImplementingInterface(Class<T> interfasse);

    void registerBean(String name, Object bean) throws DiException;

    boolean isMutable();

}
