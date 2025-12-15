package com.garganttua.core.injection.dummies;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntry;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.utils.CopyException;

public class DummyDiContext implements IDiContext {

    @Override
    public IDiContext onStart() throws LifecycleException {
        throw new UnsupportedOperationException("Unimplemented method 'onStart'");
    }

    @Override
    public IDiContext onStop() throws LifecycleException {
        throw new UnsupportedOperationException("Unimplemented method 'onStop'");
    }

    @Override
    public IDiContext onFlush() throws LifecycleException {
        throw new UnsupportedOperationException("Unimplemented method 'onFlush'");
    }

    @Override
    public IDiContext onInit() throws LifecycleException {
        throw new UnsupportedOperationException("Unimplemented method 'onInit'");
    }

    @Override
    public IDiContext onReload() throws LifecycleException {
        throw new UnsupportedOperationException("Unimplemented method 'onReload'");
    }

    @Override
    public Set<IBeanProvider> getBeanProviders() {
        throw new UnsupportedOperationException("Unimplemented method 'getBeanProviders'");
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() {
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyProviders'");
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) {
        throw new UnsupportedOperationException("Unimplemented method 'getProperty'");
    }

    @Override
    public <T> Optional<T> getProperty(String scopeName, String key, Class<T> type) {
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyFromProvider'");
    }

    @Override
    public void setProperty(String scopeName, String key, Object value) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'setPropertyInProvider'");
    }

    @Override
    public <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'newChildContext'");
    }

    @Override
    public <ChildContext extends IDiContext> Set<IDiChildContextFactory<ChildContext>> getChildContextFactories() {
        throw new UnsupportedOperationException("Unimplemented method 'getChildContextFactories'");
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'getProperty'");
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        throw new UnsupportedOperationException("Unimplemented method 'getBeanProvider'");
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        throw new UnsupportedOperationException("Unimplemented method 'getPropertyProvider'");
    }

    @Override
    public void registerChildContextFactory(IDiChildContextFactory<? extends IDiContext> factory) {
        throw new UnsupportedOperationException("Unimplemented method 'registerChildContextFactory'");
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }

    @Override
    public Set<Resolved> resolve(Executable method) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'resolve'");
    }

    @Override
    public void addResolver(Class<? extends Annotation> annotation, IElementResolver resolver) {
        throw new UnsupportedOperationException("Unimplemented method 'addResolver'");
    }

    @Override
    public IDiContext copy() throws CopyException {
        throw new UnsupportedOperationException("Unimplemented method 'copy'");
    }

    @Override
    public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
        throw new UnsupportedOperationException("Unimplemented method 'nativeConfiguration'");
    }

    @Override
    public <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanReference<Bean> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanReference<Bean> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanReference<Bean> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBean'");
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanReference<Bean> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanReference<Bean> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanReference<Bean> query) throws DiException {
        throw new UnsupportedOperationException("Unimplemented method 'queryBeans'");
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean, boolean autoDetect)
            throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBean'");
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, Optional<T> bean, boolean autoDetect)
            throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBean'");
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBean'");
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, Optional<T> bean) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBean'");
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBean'");
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, boolean autoDetect) throws DiException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addBean'");
    }



}
