package com.garganttua.injection;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    private final List<IBeanProvider> beanProviders;
    private final List<IPropertyProvider> propertyProviders;
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories;

    // --- Constructeur ---
    public DiContext(List<IBeanProvider> beanProviders, List<IPropertyProvider> propertyProviders,
            List<IDiChildContextFactory<? extends IDiContext>> childContextFactories) {
        this.beanProviders = Collections.unmodifiableList(Objects.requireNonNull(beanProviders));
        this.propertyProviders = Collections.unmodifiableList(Objects.requireNonNull(propertyProviders));
        this.childContextFactories = Collections.unmodifiableList(Objects.requireNonNull(childContextFactories));
        context = this;
    }

    // --- Getters ---
    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(beanProviders));
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        ensureInitializedAndStarted();
        return Collections.unmodifiableSet(new HashSet<>(propertyProviders));
    }

    @Override
    public Set<IDiChildContextFactory<? extends IDiContext>> getChildContextFactories() throws DiException {
        ensureInitializedAndStarted();
        return Collections
                .unmodifiableSet(new HashSet<IDiChildContextFactory<? extends IDiContext>>(childContextFactories));
    }

    // --- Gestion des beans ---
    @Override
    public <T> Optional<T> getBean(Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanProviders.stream()
                .map(provider -> provider.getBean(type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getBean(String name, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanProviders.stream()
                .map(provider -> provider.getBean(name, type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getBeanFromProvider(String providerName, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .flatMap(provider -> provider.getBean(type));
    }

    @Override
    public <T> Optional<T> getBeanFromProvider(String providerName, String name, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return beanProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .flatMap(provider -> provider.getBean(name, type));
    }

    @Override
    public <T> List<T> getBeansImplementingInterface(String providerName, Class<T> interfasse, boolean includePrototypes) throws DiException {
        ensureInitializedAndStarted();
        return beanProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .map(provider -> provider.getBeansImplementingInterface(interfasse, includePrototypes))
                .orElse(Collections.emptyList());
    }

    @Override
    public void setBeanInProvider(String providerName, String name, Object bean) throws DiException {
        ensureInitializedAndStarted();
        beanProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .filter(IBeanProvider::isMutable)
                .orElseThrow(() -> new DiException("Provider " + providerName + " not found or immutable"))
                .registerBean(name, bean);
    }

    @Override
    public void setBeanInProvider(String providerName, Object bean) throws DiException {
        ensureInitializedAndStarted();
        setBeanInProvider(providerName, bean.getClass().getName(), bean);
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return propertyProviders.stream()
                .map(provider -> provider.getProperty(key, type))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Override
    public <T> Optional<T> getPropertyFromProvider(String providerName, String key, Class<T> type) throws DiException {
        ensureInitializedAndStarted();
        return propertyProviders.stream()
                .filter(provider -> provider.getName().equals(providerName))
                .findFirst()
                .flatMap(provider -> provider.getProperty(key, type));
    }

    @Override
    public void setPropertyInProvider(String providerName, String key, Object value) throws DiException {
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

    @Override
    public <T> List<T> getBeansImplementingInterface(String providerName, Class<T> interfasse) throws DiException {
        return this.getBeansImplementingInterface(providerName, interfasse, false);
    }



    @Override
    protected ILifecycle doInit() throws DiException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) lc.onInit();
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws DiException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) lc.onStart();
        }
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws DiException {
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) lc.onFlush();
        }
        return this;
    }

    @Override
    protected ILifecycle doStop() throws DiException {
        // On arrête en ordre inverse pour respecter les dépendances potentielles
        List<Object> lifecycleObjects = new ArrayList<>(getAllLifecycleObjects());
        Collections.reverse(lifecycleObjects);
        for (Object obj : lifecycleObjects) {
            if (obj instanceof ILifecycle lc) lc.onStop();
        }
        return this;
    }

        // --- Utilitaires internes ---
    private List<Object> getAllLifecycleObjects() {
        List<Object> objs = new ArrayList<>();
        objs.addAll(beanProviders);
        objs.addAll(propertyProviders);
        objs.addAll(childContextFactories);
        return objs;
    }
}