package com.garganttua.core.injection;

import java.util.List;
import java.util.Optional;

import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.utils.Copyable;

public interface IBeanProvider extends ILifecycle, Copyable<IBeanProvider> {

    <T> Optional<T> getBean(Class<T> type) throws DiException;

    <T> Optional<T> getBean(String name, Class<T> type) throws DiException;

    <T> List<T> getBeansImplementingInterface(Class<T> interfasse, boolean includePrototypes);

    boolean isMutable();

    <T> Optional<T> queryBean(BeanDefinition<T> definition) throws DiException;

    <T> List<T> queryBeans(BeanDefinition<T> definition) throws DiException;

}
