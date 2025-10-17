package com.garganttua.injection.spec;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.spec.injection.IInjector;

public interface IDiContext extends ILifecycle, IInjector {

    // --- Bean Scopes ---
    Set<IBeanScope> getBeanScopes() throws DiException;

    <T> Optional<T> getBean(Class<T> type) throws DiException;

    <T> Optional<T> getBean(String name, Class<T> type) throws DiException;

    <T> Optional<T> getBeanFromScope(String scopeName, Class<T> type) throws DiException;

    <T> Optional<T> getBeanFromScope(String scopeName, String name, Class<T> type) throws DiException;

    <T> List<T> getBeansImplementingInterface(String scopeName, Class<T> interfasse) throws DiException;

    void setBeanInScope(String scopeName, String name, Object bean) throws DiException;

    void setBeanInScope(String scopeName, Object bean) throws DiException;

    // --- Property Scopes ---
    Set<IPropertyScope> getPropertyScopes() throws DiException;

    <T> Optional<T> getProperty(String key, Class<T> type) throws DiException;

    <T> Optional<T> getPropertyFromScope(String scopeName, String key, Class<T> type) throws DiException;

    void setPropertyInScope(String scopeName, String key, Object value) throws DiException;

    // --- Core ---

    <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass, Object... args)
            throws DiException;

    <ChildContext extends IDiContext> Set<IDiChildContextFactory<ChildContext>> getChildContextFactories() throws DiException;
}
