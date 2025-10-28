package com.garganttua.injection;

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
import java.util.stream.Collectors;

import com.garganttua.injection.beans.BeanDefinition;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.ILifecycle;
import com.garganttua.injection.spec.IPropertyProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiContext extends AbstractLifecycle implements IDiContext {

    // --- Singleton public ---
    public static IDiContext context;

    // --- Structures internes ---
    private final Map<String, IBeanProvider> beanProviders;
    private final List<IPropertyProvider> propertyProviders;
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories;

    // --- Constructeur ---
    public DiContext(List<IBeanProvider> beanProviders,
            List<IPropertyProvider> propertyProviders,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories) {

        Objects.requireNonNull(beanProviders, "beanProviders cannot be null");
        Objects.requireNonNull(propertyProviders, "propertyProviders cannot be null");
        Objects.requireNonNull(childContextFactories, "childContextFactories cannot be null");

        // Création de la Map à partir de la liste
        this.beanProviders = Collections.unmodifiableMap(
                beanProviders.stream().collect(Collectors.toMap(IBeanProvider::getName, bp -> bp)));
        this.propertyProviders = Collections.unmodifiableList(propertyProviders);
        this.childContextFactories = Collections.unmodifiableList(childContextFactories);

        context = this;
    }

    // --- Getters ---
    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(beanProviders.values()));
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(propertyProviders));
    }

    @Override
    public Set<IDiChildContextFactory<? extends IDiContext>> getChildContextFactories() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(childContextFactories));
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        if(provider.isPresent())
            return this.getProperty(provider.get(), key, type);
        return this.getProperty(key, type);
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        return propertyProviders.stream()
                .map(provider -> {
                    try {
                        return provider.getProperty(key, type);
                    } catch (DiException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return null;
                })
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        return propertyProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .flatMap(provider -> {
                    try {
                        return provider.getProperty(key, type);
                    } catch (DiException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return null;
                });
    }

    @Override
    public void setProperty(String providerName, String key, Object value) throws DiException {
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(value, "Value cannnot be null");
        ensureInitializedAndStarted();
        propertyProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .filter(IPropertyProvider::isMutable)
                .orElseThrow(() -> new DiException("PropertyProvider " + providerName + " not found or immutable"))
                .setProperty(key, value);
    }

    @Override
    public <ChildContext extends IDiContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args)
            throws DiException {
        ensureInitializedAndStarted();
        return childContextFactories.stream()
                .filter(factory -> {
                    Class<? extends IDiContext> childType = getChildContextType(factory);
                    return childType != null && contextClass.isAssignableFrom(childType);
                })
                .findFirst()
                .map(factory -> {
                    try {
                        return contextClass.cast(factory.createChildContext(this, args));
                    } catch (DiException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElseThrow(() -> new DiException(
                        "No child context factory registered for context class " + contextClass.getName()));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IDiContext> getChildContextType(
            IDiChildContextFactory<? extends IDiContext> factory) {
        Type[] genericInterfaces = factory.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType pt
                    && pt.getRawType() instanceof Class<?> raw
                    && IDiChildContextFactory.class.isAssignableFrom(raw)) {
                Type actual = pt.getActualTypeArguments()[0];
                if (actual instanceof Class<?> clazz) {
                    return (Class<? extends IDiContext>) clazz;
                }
            }
        }
        return null;
    }

    // --- Cycle de vie ---
    @Override
    protected ILifecycle doInit() throws DiException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc)
                lc.onInit();
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws DiException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc)
                lc.onStart();
        }
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws DiException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc)
                lc.onFlush();
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws DiException {
        List<Object> lifecycleObjects = new ArrayList<>(getAllLifecycleObjects());
        Collections.reverse(lifecycleObjects);
        for (Object obj : lifecycleObjects) {
            if (obj instanceof ILifecycle lc)
                lc.onStop();
        }
        return this;
    }

    private List<Object> getAllLifecycleObjects() {
        List<Object> objs = new ArrayList<>();
        objs.addAll(beanProviders.values());
        objs.addAll(propertyProviders);
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

}