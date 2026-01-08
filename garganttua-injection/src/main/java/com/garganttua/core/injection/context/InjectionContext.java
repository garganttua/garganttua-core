package com.garganttua.core.injection.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.injection.context.dsl.InjectionContextBuilder;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.utils.CopyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InjectionContext extends AbstractLifecycle implements IInjectionContext {

    public volatile static IInjectionContext context = null;

    private final Map<String, IBeanProvider> beanProviders;
    private final Map<String, IPropertyProvider> propertyProviders;
    private final List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories;

    private IInjectableElementResolver resolverDelegate;

    private final Object mutex = new Object();
    private final Object copyMutex = new Object();
    private final Object singletonMutex = new Object();

    public static IInjectionContextBuilder builder() throws DslException {
        log.atTrace().log("Entering InjectionContext.builder()");
        IInjectionContextBuilder builder = new InjectionContextBuilder();
        log.atTrace().log("Exiting InjectionContext.builder()");
        return builder;
    }

    public static IInjectionContext master(IInjectableElementResolver resolver,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories) {
        log.atTrace().log("Creating master InjectionContext");
        IInjectionContext ctx = new InjectionContext(true, resolver, beanProviders, propertyProviders, childContextFactories);
        log.atInfo().log("Master InjectionContext created");
        return ctx;
    }

    public static IInjectionContext child(IInjectableElementResolver resolver,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories) {
        log.atTrace().log("Creating child InjectionContext");
        IInjectionContext ctx = new InjectionContext(false, resolver, beanProviders, propertyProviders, childContextFactories);
        log.atInfo().log("Child InjectionContext created");
        return ctx;
    }

    protected InjectionContext(Boolean masterContext, IInjectableElementResolver resolver,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories) {

        log.atTrace().log("Initializing InjectionContext");
        this.beanProviders = Collections
                .synchronizedMap(new HashMap<>(Objects.requireNonNull(beanProviders, "beanProviders cannot be null")));
        this.propertyProviders = Collections.synchronizedMap(
                new HashMap<>(Objects.requireNonNull(propertyProviders, "propertyProviders cannot be null")));
        this.childContextFactories = Collections.synchronizedList(
                new ArrayList<>(Objects.requireNonNull(childContextFactories, "childContextFactories cannot be null")));

        this.resolverDelegate = Objects.requireNonNull(resolver, "Resolver cannot be null");
        log.atDebug().log("Resolver delegate set, beanProviders: {}, propertyProviders: {}, childFactories: {}",
                beanProviders.keySet(), propertyProviders.keySet(), childContextFactories.size());

        if (masterContext) {
            log.atDebug().log("Setting up master context singleton");
            setupMasterContextSingleton();
        }
        log.atTrace().log("InjectionContext initialized");
    }

    private void setupMasterContextSingleton() {
        synchronized (this.singletonMutex) {
            InjectionContext.context = this;
            log.atInfo().log("Master context singleton assigned");
        }
    }

    // --- Getters ---
    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        log.atTrace().log("Getting bean providers");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Set<IBeanProvider> result = Collections.unmodifiableSet(new HashSet<>(beanProviders.values()));
        log.atDebug().log("Returning {} bean providers", result.size());
        return result;
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        log.atTrace().log("Getting property providers");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Set<IPropertyProvider> result = Collections.unmodifiableSet(new HashSet<>(propertyProviders.values()));
        log.atDebug().log("Returning {} property providers", result.size());
        return result;
    }

    @Override
    public Set<IInjectionChildContextFactory<? extends IInjectionContext>> getChildContextFactories() throws DiException {
        log.atTrace().log("Getting child context factories");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Set<IInjectionChildContextFactory<? extends IInjectionContext>> result = Collections.unmodifiableSet(
                new HashSet<>(childContextFactories));
        log.atDebug().log("Returning {} child context factories", result.size());
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, Class<T> type) throws DiException {
        log.atTrace().log("Getting property with Optional provider: {}, key: {}, type: {}", provider, key, type);
        Optional<T> result;
        if (provider.isPresent()) {
            result = this.getProperty(provider.get(), key, type);
        } else {
            result = this.getProperty(key, type);
        }
        log.atDebug().log("Property lookup result: {}", result);
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(String key, Class<T> type) throws DiException {
        log.atTrace().log("Getting property with key: {}, type: {}", key, type);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        Optional<T> result = propertyProviders.values().stream()
                .map(provider -> provider.getProperty(key, type))
                .flatMap(Optional::stream)
                .findFirst();
        log.atDebug().log("Property value found: {}", result);
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, Class<T> type) throws DiException {
        log.atTrace().log("Getting property from provider: {}, key: {}, type: {}", providerName, key, type);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        Optional<T> result = propertyProviders.entrySet().stream()
                .filter(entry -> entry.getKey().equals(providerName))
                .findFirst()
                .flatMap(entry -> entry.getValue().getProperty(key, type));
        log.atDebug().log("Property value found from provider {}: {}", providerName, result);
        return result;
    }

    @Override
    public void setProperty(String providerName, String key, Object value) throws DiException {
        log.atTrace().log("Setting property for provider: {}, key: {}, value: {}", providerName, key, value);
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(value, "Value cannnot be null");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        propertyProviders.entrySet().stream()
                .filter(entry -> entry.getKey().equals(providerName))
                .findFirst()
                .filter(entry -> entry.getValue().isMutable())
                .orElseThrow(() -> {
                    log.atError().log("Failed to set property. Provider {} not found or immutable", providerName);
                    return new DiException("PropertyProvider " + providerName + " not found or immutable");
                })
                .getValue().setProperty(key, value);
        log.atInfo().log("Property set successfully for provider: {}, key: {}", providerName, key);
    }

    @Override
    public <ChildContext extends IInjectionContext> ChildContext newChildContext(Class<ChildContext> contextClass,
            Object... args) throws DiException {
        log.atTrace().log("Creating new child context of type: {}", contextClass.getName());
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        synchronized (this.mutex) {
            ChildContext ctx = childContextFactories.stream()
                    .filter(factory -> {
                        Class<? extends IInjectionContext> childType = getChildContextType(factory);
                        return childType != null && contextClass.isAssignableFrom(childType);
                    })
                    .findFirst()
                    .map(factory -> contextClass.cast(factory.createChildContext((IInjectionContext) this.copy(), args)))
                    .orElseThrow(() -> {
                        log.atError().log("No child context factory registered for class {}", contextClass.getName());
                        return new DiException(
                                "No child context factory registered for context class " + contextClass.getName());
                    });
            log.atInfo().log("Child context created: {}", ctx);
            return ctx;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IInjectionContext> getChildContextType(
            IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.atTrace().log("Getting child context type for factory {}", factory);
        Type[] genericInterfaces = factory.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (isParameterizedOf(type, IInjectionChildContextFactory.class)) {
                Type actual = ((ParameterizedType) type).getActualTypeArguments()[0];
                if (isParameterizedOf(actual, IInjectionContext.class)) {
                    Class<? extends IInjectionContext> clazz = (Class<? extends IInjectionContext>) ((ParameterizedType) actual).getRawType();
                    log.atDebug().log("Child context type determined: {}", clazz);
                    return clazz;
                }
            }
        }
        log.atWarn().log("Could not determine child context type for factory {}", factory);
        return null;
    }

    private static boolean isParameterizedOf(Type type, Class<?> interfasse) {
        return (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> raw
                && interfasse.isAssignableFrom(raw));
    }

    // --- Lifecycle methods ---
    @Override
    protected ILifecycle doInit() throws LifecycleException {
        log.atTrace().log("Initializing lifecycle objects");
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) {
                lc.onInit();
                log.atDebug().log("Initialized lifecycle object: {}", obj);
            }
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        log.atTrace().log("Starting lifecycle objects");
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) {
                lc.onStart();
                log.atDebug().log("Started lifecycle object: {}", obj);
            }
        }
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        log.atTrace().log("Flushing lifecycle objects and clearing providers");
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) {
                lc.onFlush();
                log.atDebug().log("Flushed lifecycle object: {}", obj);
            }
        }
        this.beanProviders.clear();
        this.propertyProviders.clear();
        log.atInfo().log("Providers cleared");
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        log.atTrace().log("Stopping lifecycle objects");
        List<Object> lifecycleObjects = new ArrayList<>(getAllLifecycleObjects());
        for (Object obj : lifecycleObjects.reversed()) {
            if (obj instanceof ILifecycle lc) {
                lc.onStop();
                log.atDebug().log("Stopped lifecycle object: {}", obj);
            }
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
    public <Bean> Optional<Bean> queryBean(Optional<String> provider, BeanReference<Bean> query)
            throws DiException {
        log.atTrace().log("Querying bean with Optional provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(query, "Bean query cannot be null");
        Optional<Bean> result = provider.isPresent()
                ? this.queryBean(provider.get(), query)
                : this.queryBean(query);
        log.atDebug().log("Bean query result: {}", result);
        return result;
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanReference<Bean> query) throws DiException {
        log.atTrace().log("Querying bean from provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null) {
            log.atError().log("Invalid bean provider: {}", provider);
            throw new DiException("Invalid bean provider " + provider);
        }
        Optional<Bean> result = beanProvider.query(query);
        log.atDebug().log("Bean obtained from provider {}: {}", provider, result);
        return result;
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanReference<Bean> query) throws DiException {
        log.atTrace().log("Querying bean from all providers, query: {}", query);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Objects.requireNonNull(query, "Bean query cannot be null");
        for (IBeanProvider provider : this.beanProviders.values()) {
            Optional<Bean> bean = provider.query(query);
            if (bean.isPresent()) {
                log.atDebug().log("Bean found in provider {}: {}", provider, bean);
                return bean;
            }
        }
        log.atDebug().log("No bean found for query {}", query);
        return Optional.empty();
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanReference<Bean> query) throws DiException {
        log.atTrace().log("Querying beans with Optional provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(query, "Bean query cannot be null");
        List<Bean> result = provider.isPresent()
                ? this.queryBeans(provider.get(), query)
                : this.queryBeans(query);
        log.atDebug().log("Beans query result: {} items", result.size());
        return result;
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanReference<Bean> query) throws DiException {
        log.atTrace().log("Querying beans from all providers, query: {}", query);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Objects.requireNonNull(query, "Bean query cannot be null");
        List<Bean> beans = new ArrayList<>();
        for (IBeanProvider provider : this.beanProviders.values()) {
            List<Bean> providerBeans = provider.queries(query);
            beans.addAll(providerBeans);
            log.atDebug().log("Found {} beans in provider {}", providerBeans.size(), provider);
        }
        log.atDebug().log("Total beans found: {}", beans.size());
        return beans;
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanReference<Bean> query) throws DiException {
        log.atTrace().log("Querying beans from provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null) {
            log.atError().log("Invalid bean provider: {}", provider);
            throw new DiException("Invalid bean provider " + provider);
        }
        List<Bean> result = beanProvider.queries(query);
        log.atDebug().log("Beans obtained from provider {}: {} items", provider, result.size());
        return result;
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        log.atTrace().log("Getting bean provider: {}", name);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Optional<IBeanProvider> result = Optional.ofNullable(this.beanProviders.get(name));
        log.atDebug().log("Bean provider found: {}", result);
        return result;
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        log.atTrace().log("Getting property provider: {}", name);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Optional<IPropertyProvider> result = Optional.ofNullable(this.propertyProviders.get(name));
        log.atDebug().log("Property provider found: {}", result);
        return result;
    }

    public Map<String, IBeanProvider> beanProviders() {
        log.atTrace().log("Accessing beanProviders map");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        return this.beanProviders;
    }

    public Map<String, IPropertyProvider> propertyProviders() {
        log.atTrace().log("Accessing propertyProviders map");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        return this.propertyProviders;
    }

    public List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories() {
        log.atTrace().log("Accessing childContextFactories list");
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        return this.childContextFactories;
    }

    @Override
    public void registerChildContextFactory(IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.atTrace().log("Registering child context factory: {}", factory);
        wrapLifecycle(this::ensureInitialized, DiException.class);
        synchronized (this.mutex) {
            Objects.requireNonNull(factory, "Factory cannot be null");
            if (childContextFactories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
                childContextFactories.add(factory);
                log.atInfo().log("Child context factory registered: {}", factory);
            } else {
                log.atWarn().log("Child context factory already registered: {}", factory);
            }
        }
    }

    @Override
    public Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException {
        log.atTrace().log("Resolving element: {}", element);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Resolved result = this.resolverDelegate.resolve(elementType, element);
        log.atDebug().log("Resolved element: {}", result);
        return result;
    }

    @Override
    public Set<Resolved> resolve(Executable method) throws DiException {
        log.atTrace().log("Resolving method: {}", method);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        Set<Resolved> result = this.resolverDelegate.resolve(method);
        log.atDebug().log("Resolved method: {} items", result.size());
        return result;
    }

    @Override
    public void addResolver(Class<? extends Annotation> annotation, IElementResolver resolver) {
        log.atTrace().log("Adding resolver for annotation: {}", annotation);
        wrapLifecycle(this::ensureInitializedAndStarted, DiException.class);
        synchronized (this.mutex) {
            this.resolverDelegate.addResolver(annotation, resolver);
            log.atInfo().log("Resolver added for annotation: {}", annotation);
        }
    }

    @Override
    public IInjectionContext copy() throws CopyException {
        log.atTrace().log("Copying InjectionContext");
        wrapLifecycle(this::ensureInitializedAndStarted, CopyException.class);
        synchronized (this.copyMutex) {
            Map<String, IBeanProvider> beanProvidersCopy = this.beanProviders.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));

            Map<String, IPropertyProvider> propertyProvidersCopy = this.propertyProviders.entrySet().stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().copy()));

            List<IInjectionChildContextFactory<? extends IInjectionContext>> childFactoriesCopy = new ArrayList<>(this.childContextFactories);

            IInjectionContext copy = InjectionContext.child(
                    this.resolverDelegate,
                    new HashMap<>(beanProvidersCopy),
                    new HashMap<>(propertyProvidersCopy),
                    new ArrayList<>(childFactoriesCopy));

            log.atInfo().log("InjectionContext copied successfully");
            return copy;
        }
    }

    @Override
    public Set<IReflectionConfigurationEntryBuilder> nativeConfiguration() {
                return this.getBeanProvider(Predefined.BeanProviders.garganttua.toString())
            .map(IBeanProvider::nativeConfiguration)
            .orElse(Collections.emptySet());
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean, boolean autoDetect)
            throws DiException {
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null) {
            log.atError().log("Invalid bean provider: {}", provider);
            throw new DiException("Invalid bean provider " + provider);
        }
        beanProvider.add(reference, bean, autoDetect);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, Optional<T> bean, boolean autoDetect)
            throws DiException {
        this.addBean(provider, reference, bean.orElseGet(null), autoDetect);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean) throws DiException {
        this.addBean(provider, reference, bean, false);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, Optional<T> bean) throws DiException {
        this.addBean(provider, reference, bean.orElseGet(null), false);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference) throws DiException {
        this.addBean(provider, reference, Optional.empty(), false);
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, boolean autoDetect) throws DiException {
        this.addBean(provider, reference, Optional.empty(), autoDetect);
    }
}
