package com.garganttua.core.injection.context;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.dsl.DiContextBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiContext extends AbstractLifecycle implements IDiContext {

    // --- Singleton public ---
    public static IDiContext context;

    // --- Structures internes ---
    private final Map<String, IBeanProvider> beanProviders;
    private final Map<String, IPropertyProvider> propertyProviders;
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories;

    private IInjectableElementResolver resolverDelegate;

    public static IDiContextBuilder builder() throws DslException {
        return new DiContextBuilder();
    }

    // --- Constructeur ---
    public DiContext(IInjectableElementResolver resolver, Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories) {

        this.beanProviders = Objects.requireNonNull(beanProviders, "beanProviders cannot be null");
        this.propertyProviders = Objects.requireNonNull(propertyProviders, "propertyProviders cannot be null");
        this.resolverDelegate = Objects.requireNonNull(resolver, "Resolver cannot be null");
        Objects.requireNonNull(childContextFactories, "childContextFactories cannot be null");

        this.childContextFactories = childContextFactories;

        DiContext.context = this;
    }

    // --- Getters ---
    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        try {
            ensureInitializedAndStarted();
        } catch (LifecycleException e) {
            throw new DiException(e);
        }
        return Collections.unmodifiableSet(new HashSet<>(beanProviders.values()));
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        try {
            ensureInitializedAndStarted();
        } catch (LifecycleException e) {
            throw new DiException(e);
        }
        return Collections.unmodifiableSet(new HashSet<>(propertyProviders.values()));
    }

    @Override
    public Set<IDiChildContextFactory<? extends IDiContext>> getChildContextFactories() throws DiException {
        try {
            ensureInitializedAndStarted();
        } catch (LifecycleException e) {
            throw new DiException(e);
        }
        return Collections.unmodifiableSet(new HashSet<>(childContextFactories));
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        if (provider.isPresent())
            return this.getProperty(provider.get(), key, type);
        return this.getProperty(key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        wrapLifecycle(this::ensureInitializedAndStarted);
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        return propertyProviders.values().stream()
                .map(provider -> provider.getProperty(key, type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException {
        wrapLifecycle(this::ensureInitializedAndStarted);
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        return propertyProviders.entrySet().stream()
                .filter(entry -> entry.getKey().equals(providerName))
                .findFirst()
                .flatMap(entry -> entry.getValue().getProperty(key, type));
    }

    @Override
    public void setProperty(String providerName, String key, Object value) throws DiException {
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(value, "Value cannnot be null");
        wrapLifecycle(this::ensureInitializedAndStarted);
        propertyProviders.entrySet().stream()
                .filter(entry -> entry.getKey().equals(providerName))
                .findFirst()
                .filter(entry -> entry.getValue().isMutable())
                .orElseThrow(() -> new DiException("PropertyProvider " + providerName + " not found or immutable"))
                .getValue().setProperty(key, value);
    }

    @Override
    public <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args)
            throws DiException {
        wrapLifecycle(this::ensureInitializedAndStarted);
        return childContextFactories.stream()
                .filter(factory -> {
                    Class<? extends IDiContext> childType = getChildContextType(factory);
                    return childType != null && contextClass.isAssignableFrom(childType);
                })
                .findFirst()
                .map(factory -> contextClass.cast(factory.createChildContext(this, args)))
                .orElseThrow(() -> new DiException(
                        "No child context factory registered for context class " + contextClass.getName()));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IDiContext> getChildContextType(
            IDiChildContextFactory<? extends IDiContext> factory) {
        Type[] genericInterfaces = factory.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {

            if ( isParameterizedOf(type, IDiChildContextFactory.class) ) {

                Type actual = ((ParameterizedType) type).getActualTypeArguments()[0];

                if (isParameterizedOf(actual, IDiContext.class)) {

                    return (Class<? extends IDiContext>) ((ParameterizedType) actual).getRawType();
                }
            }
        }
        return null;
    }

    private static boolean isParameterizedOf(Type type, Class<?> interfasse){
        return (type instanceof ParameterizedType parameterizedType
                    && parameterizedType.getRawType() instanceof Class<?> raw
                    && interfasse.isAssignableFrom(raw));
    }

    // --- Cycle de vie ---
    @Override
    protected ILifecycle doInit() throws LifecycleException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc)
                lc.onInit();
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc)
                lc.onStart();
        }
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc)
                lc.onFlush();
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        List<Object> lifecycleObjects = new ArrayList<>(getAllLifecycleObjects());
        for (Object obj : lifecycleObjects.reversed()) {
            if (obj instanceof ILifecycle lc)
                lc.onStop();
        }
        return this;
    }

    private List<Object> getAllLifecycleObjects() {
        List<Object> objs = new ArrayList<>();
        objs.addAll(beanProviders.values());
        objs.addAll(propertyProviders.values());
        objs.addAll(childContextFactories);
        return objs;
    }

    @Override
    public <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanDefinition<Bean> definition)
            throws DiException {
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(definition, "Bean definition cannot be null");
        if (provider.isPresent())
            return this.queryBean(provider.get(), definition);
        return this.queryBean(definition);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanDefinition<Bean> definition) throws DiException {
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null)
            throw new DiException("Invalid bean provider " + provider);

        return beanProvider.queryBean(definition);
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanDefinition<Bean> definition) throws DiException {
        Objects.requireNonNull(definition, "Bean definition cannot be null");

        for (IBeanProvider provider : this.beanProviders.values()) {
            Optional<Bean> bean = provider.queryBean(definition);
            if (bean.isPresent()) {
                return bean;
            }
        }
        return Optional.empty();
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanDefinition<Bean> definition) throws DiException {
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(definition, "Bean definition cannot be null");
        if (provider.isPresent())
            return this.queryBeans(provider.get(), definition);
        return this.queryBeans(definition);
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanDefinition<Bean> definition) throws DiException {
        Objects.requireNonNull(definition, "Bean definition cannot be null");

        List<Bean> beans = new ArrayList<>();
        for (IBeanProvider provider : this.beanProviders.values()) {
            beans.addAll(provider.queryBeans(definition)); 
        }
        return beans;
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanDefinition<Bean> definition) throws DiException {
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null)
            throw new DiException("Invalid bean provider " + provider);

        return beanProvider.queryBeans(definition);
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        return Optional.ofNullable(this.beanProviders.get(name));
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        return Optional.ofNullable(this.propertyProviders.get(name));
    }

    private void wrapLifecycle(RunnableWithException runnable) throws DiException {
        try {
            runnable.run();
        } catch (LifecycleException e) {
            throw new DiException(e);
        }
    }

    @FunctionalInterface
    interface RunnableWithException {
        void run() throws LifecycleException;
    }

    public Map<String, IBeanProvider> beanProviders() {
        return this.beanProviders;
    }

    public Map<String, IPropertyProvider> propertyProviders() {
        return this.propertyProviders;
    }

    public List<IDiChildContextFactory<? extends IDiContext>> childContextFactories() {
        return this.childContextFactories;
    }

    @Override
    public void registerChildContextFactory(IDiChildContextFactory<? extends IDiContext> factory) {
        Objects.requireNonNull(factory, "Factory cannot be null");
        if (childContextFactories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
            childContextFactories.add(factory);
        }
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        return this.resolverDelegate.resolve(elementType, element);
    }

    @Override
    public Set<Resolved> resolve(Executable method) throws DiException {
        return this.resolverDelegate.resolve(method);
    }


}