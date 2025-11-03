package com.garganttua.injection.spec;

import java.util.List;
import java.util.Optional;

import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanDefinition;

public interface IBeanProvider extends ILifecycle {

    <T> Optional<T> getBean(Class<T> type) throws DiException;

    <T> Optional<T> getBean(String name, Class<T> type) throws DiException;

    <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes);

    /* void registerBean(String name, Object bean) throws DiException; */

    boolean isMutable();

    <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException;

}
