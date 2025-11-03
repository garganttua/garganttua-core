package com.garganttua.injection.spec;

import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.DiException;
import com.garganttua.injection.beans.BeanDefinition;

public interface IDiContext extends ILifecycle {

        // --- Bean Scopes ---
        Set<IBeanProvider> getBeanProviders() throws DiException;

        Optional<IBeanProvider> getBeanProvider(String name);

        <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException;

        <Bean> Optional<Bean> queryBean(BeanDefinition<Bean> definition) throws DiException;

        <Bean> Optional<Bean> queryBean(String provider, BeanDefinition<Bean> definition) throws DiException;

        // --- Property Scopes ---
        Set<IPropertyProvider> getPropertyProviders() throws DiException;

        Optional<IPropertyProvider> getPropertyProvider(String name);

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
