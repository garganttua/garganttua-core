package com.garganttua.injection.spec;

import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanDefinition;

public interface IDiContext extends ILifecycle {

    // --- Bean Scopes ---
    Set<IBeanProvider> getBeanProviders() throws DiException;

    <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException;

    <Bean> Optional<Bean> queryBean(BeanDefinition<Bean> definition) throws DiException;

    <Bean> Optional<Bean> queryBean(String provider, BeanDefinition<Bean> definition) throws DiException;

    /* <T> Optional<T> getBean(Class<T> type) throws DiException;

    <T> Optional<T> getBean(String name, Class<T> type) throws DiException;

    <T> Optional<T> getBeanFromProvider(String providerName, Class<T> type) throws DiException;

    <T> Optional<T> getBeanFromProvider(String providerName, String name, Class<T> type) throws DiException;

    <T> List<T> getBeansImplementingInterface(String providerName, Class<T> interfasse) throws DiException;

    <T> List<T> getBeansImplementingInterface(String providerName, Class<T> interfasse, boolean includePrototypes)
            throws DiException;

    void setBeanInProvider(String providerName, String name, Object bean) throws DiException;

    void setBeanInProvider(String providerName, Object bean) throws DiException; */

    // --- Property Scopes ---
    Set<IPropertyProvider> getPropertyProviders() throws DiException;

    <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException;

    <T> Optional<T> getProperty(String key, Class<T> type) throws DiException;

    <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException;

    void setProperty(String provider, String key, Object value) throws DiException;

    // --- Core ---

    <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass, Object... args)
            throws DiException;

    <ChildContext extends IDiContext> Set<IDiChildContextFactory<ChildContext>> getChildContextFactories()
            throws DiException;

}
