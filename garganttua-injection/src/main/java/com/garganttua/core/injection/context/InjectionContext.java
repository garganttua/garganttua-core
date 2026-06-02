package com.garganttua.core.injection.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.bootstrap.banner.IBootstrapSummaryContributor;
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
import com.garganttua.core.reflection.IAnnotatedElement;
import com.garganttua.core.reflection.IExecutable;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.lifecycle.AbstractLifecycle;
import com.garganttua.core.lifecycle.ILifecycle;
import com.garganttua.core.lifecycle.LifecycleException;
import com.garganttua.core.nativve.IReflectionConfigurationEntryBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.utils.CopyException;

/**
 * The DI context — cohesive implementation of {@link IInjectionContext} over its
 * bean/property providers, lifecycle and child contexts.
 *
 * <p><b>Size note:</b> this type exceeds the 500-line code-size gate on purpose.
 * It is a contract facade (~25 small interface methods, none over the method-length
 * limit) with no extractable complex cluster; splitting it would only add
 * forwarding boilerplate and couple a delegate to the lifecycle gating. Treated as
 * a documented exception, like the {@code IClass}/{@code IRuntimeContext} mirror
 * implementations.
 */
public class InjectionContext extends AbstractLifecycle implements IInjectionContext, IBootstrapSummaryContributor {
    private static final Logger log = Logger.getLogger(InjectionContext.class);

    public volatile static IInjectionContext context = null;

    private final Map<String, IBeanProvider> beanProviders;
    private final Map<String, IPropertyProvider> propertyProviders;
    private final List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories;

    private IInjectableElementResolver resolverDelegate;

    private final Object mutex = new Object();
    private final Object copyMutex = new Object();
    private final Object singletonMutex = new Object();

    /**
     * Creates a fresh {@link IInjectionContextBuilder} for fluent context configuration.
     *
     * @return a new context builder
     * @throws DslException if the builder cannot be initialised
     */
    public static IInjectionContextBuilder builder() throws DslException {
        log.trace("Entering InjectionContext.builder()");
        IInjectionContextBuilder builder = new InjectionContextBuilder();
        log.trace("Exiting InjectionContext.builder()");
        return builder;
    }

    /**
     * Builds the master (root) injection context, which is also installed as the
     * process-wide {@link #context} singleton.
     *
     * @return the master context
     */
    public static IInjectionContext master(IInjectableElementResolver resolver,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories) {
        log.trace("Creating master InjectionContext");
        IInjectionContext ctx = new InjectionContext(true, resolver, beanProviders, propertyProviders, childContextFactories);
        log.debug("Master InjectionContext created");
        return ctx;
    }

    /**
     * Builds a child injection context that does not become the master singleton.
     *
     * @return the child context
     */
    public static IInjectionContext child(IInjectableElementResolver resolver,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories) {
        log.trace("Creating child InjectionContext");
        IInjectionContext ctx = new InjectionContext(false, resolver, beanProviders, propertyProviders, childContextFactories);
        log.debug("Child InjectionContext created");
        return ctx;
    }

    /**
     * Builds a context over the supplied providers, factories and resolver, optionally
     * registering itself as the master singleton.
     *
     * @param masterContext {@code true} to install this instance as the master singleton
     */
    protected InjectionContext(Boolean masterContext, IInjectableElementResolver resolver,
            Map<String, IBeanProvider> beanProviders,
            Map<String, IPropertyProvider> propertyProviders,
            List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories) {

        log.trace("Initializing InjectionContext");
        this.beanProviders = Collections
                .synchronizedMap(new HashMap<>(Objects.requireNonNull(beanProviders, "beanProviders cannot be null")));
        this.propertyProviders = Collections.synchronizedMap(
                new HashMap<>(Objects.requireNonNull(propertyProviders, "propertyProviders cannot be null")));
        this.childContextFactories = Collections.synchronizedList(
                new ArrayList<>(Objects.requireNonNull(childContextFactories, "childContextFactories cannot be null")));

        this.resolverDelegate = Objects.requireNonNull(resolver, "Resolver cannot be null");
        log.debug("Resolver delegate set, beanProviders: {}, propertyProviders: {}, childFactories: {}",
                beanProviders.keySet(), propertyProviders.keySet(), childContextFactories.size());

        if (masterContext) {
            log.debug("Setting up master context singleton");
            setupMasterContextSingleton();
        }
        log.trace("InjectionContext initialized");
    }

    private void setupMasterContextSingleton() {
        synchronized (this.singletonMutex) {
            InjectionContext.context = this;
            log.debug("Master context singleton assigned");
        }
    }

    // --- Getters ---
    @Override
    public Set<IBeanProvider> getBeanProviders() throws DiException {
        log.trace("Getting bean providers");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Set<IBeanProvider> result = Collections.unmodifiableSet(new HashSet<>(beanProviders.values()));
        log.debug("Returning {} bean providers", result.size());
        return result;
    }

    @Override
    public Set<IPropertyProvider> getPropertyProviders() throws DiException {
        log.trace("Getting property providers");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Set<IPropertyProvider> result = Collections.unmodifiableSet(new HashSet<>(propertyProviders.values()));
        log.debug("Returning {} property providers", result.size());
        return result;
    }

    @Override
    public Set<IInjectionChildContextFactory<? extends IInjectionContext>> getChildContextFactories() throws DiException {
        log.trace("Getting child context factories");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Set<IInjectionChildContextFactory<? extends IInjectionContext>> result = Collections.unmodifiableSet(
                new HashSet<>(childContextFactories));
        log.debug("Returning {} child context factories", result.size());
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(Optional<String> provider, String key, IClass<T> type) throws DiException {
        log.trace("Getting property with Optional provider: {}, key: {}, type: {}", provider, key, type);
        Optional<T> result;
        if (provider.isPresent()) {
            result = this.getProperty(provider.get(), key, type);
        } else {
            result = this.getProperty(key, type);
        }
        log.debug("Property lookup result: {}", result);
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(String key, IClass<T> type) throws DiException {
        log.trace("Getting property with key: {}, type: {}", key, type);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        Optional<T> result = propertyProviders.values().stream()
                .map(provider -> provider.getProperty(key, type))
                .flatMap(Optional::stream)
                .findFirst();
        log.debug("Property value found: {}", result);
        return result;
    }

    @Override
    public <T> Optional<T> getProperty(String providerName, String key, IClass<T> type) throws DiException {
        log.trace("Getting property from provider: {}, key: {}, type: {}", providerName, key, type);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Objects.requireNonNull(providerName, "Provider cannnot be null");
        Objects.requireNonNull(key, "Key cannnot be null");
        Objects.requireNonNull(type, "Type cannnot be null");
        Optional<T> result = propertyProviders.entrySet().stream()
                .filter(entry -> entry.getKey().equals(providerName))
                .findFirst()
                .flatMap(entry -> entry.getValue().getProperty(key, type));
        log.debug("Property value found from provider {}: {}", providerName, result);
        return result;
    }

    @Override
    public void setProperty(String providerName, String key, Object value) throws DiException {
        log.trace("Setting property for provider: {}, key: {}, value: {}", providerName, key, value);
        Objects.requireNonNull(providerName, "Provider cannot be null");
        Objects.requireNonNull(key, "Key cannot be null");
        Objects.requireNonNull(value, "Value cannot be null for property '" + key + "' (provider: " + providerName + ")");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        propertyProviders.entrySet().stream()
                .filter(entry -> entry.getKey().equals(providerName))
                .findFirst()
                .filter(entry -> entry.getValue().isMutable())
                .orElseThrow(() -> {
                    log.error("Failed to set property. Provider {} not found or immutable", providerName);
                    return new DiException("PropertyProvider " + providerName + " not found or immutable");
                })
                .getValue().setProperty(key, value);
        log.debug("Property set successfully for provider: {}, key: {}", providerName, key);
    }

    @Override
    public <ChildContext extends IInjectionContext> ChildContext newChildContext(IClass<ChildContext> contextClass,
            Object... args) throws DiException {
        log.trace("Creating new child context of type: {}", contextClass.getName());
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        synchronized (this.mutex) {
            ChildContext ctx = childContextFactories.stream()
                    .filter(factory -> {
                        Class<? extends IInjectionContext> childType = getChildContextType(factory);
                        return childType != null && contextClass.isAssignableFrom(childType);
                    })
                    .findFirst()
                    .map(factory -> contextClass.cast(factory.createChildContext(this.copy(), args)))
                    .orElseThrow(() -> {
                        log.error("No child context factory registered for class {}", contextClass.getName());
                        return new DiException(
                                "No child context factory registered for context class " + contextClass.getName());
                    });
            log.debug("Child context created: {}", ctx);
            return ctx;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends IInjectionContext> getChildContextType(
            IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.trace("Getting child context type for factory {}", factory);
        Type[] genericInterfaces = factory.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (isParameterizedOf(type, IInjectionChildContextFactory.class)) {
                Type actual = ((ParameterizedType) type).getActualTypeArguments()[0];
                if (isParameterizedOf(actual, IInjectionContext.class)) {
                    Class<? extends IInjectionContext> clazz = (Class<? extends IInjectionContext>) ((ParameterizedType) actual).getRawType();
                    log.debug("Child context type determined: {}", clazz);
                    return clazz;
                }
            }
        }
        log.warn("Could not determine child context type for factory {}", factory);
        return null;
    }

    private static boolean isParameterizedOf(Type type, Class<?> interfasse) {
        return (type instanceof ParameterizedType parameterizedType
                && parameterizedType.getRawType() instanceof Class<?> raw
                && interfasse.isAssignableFrom(raw));
    }

    @Override
    public IReflection reflection() {
        return IClass.getReflection();
    }

    // --- Lifecycle methods ---
    @Override
    protected ILifecycle doInit() throws LifecycleException {
        log.trace("Initializing lifecycle objects");
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) {
                lc.onInit();
                log.debug("Initialized lifecycle object: {}", obj);
            }
        }
        return this;
    }

    @Override
    protected ILifecycle doStart() throws LifecycleException {
        log.trace("Starting lifecycle objects");
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) {
                lc.onStart();
                log.debug("Started lifecycle object: {}", obj);
            }
        }
        return this;
    }

    @Override
    protected ILifecycle doFlush() throws LifecycleException {
        log.trace("Flushing lifecycle objects and clearing providers");
        for (Object obj : getAllLifecycleObjects()) {
            if (obj instanceof ILifecycle lc) {
                lc.onFlush();
                log.debug("Flushed lifecycle object: {}", obj);
            }
        }
        this.beanProviders.clear();
        this.propertyProviders.clear();
        log.debug("Providers cleared");
        return this;
    }

    @Override
    protected ILifecycle doStop() throws LifecycleException {
        log.trace("Stopping lifecycle objects");
        List<Object> lifecycleObjects = new ArrayList<>(getAllLifecycleObjects());
        for (Object obj : lifecycleObjects.reversed()) {
            if (obj instanceof ILifecycle lc) {
                lc.onStop();
                log.debug("Stopped lifecycle object: {}", obj);
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
        log.trace("Querying bean with Optional provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(query, "Bean query cannot be null");
        Optional<Bean> result = provider.isPresent()
                ? this.queryBean(provider.get(), query)
                : this.queryBean(query);
        log.debug("Bean query result: {}", result);
        return result;
    }

    @Override
    public <Bean> Optional<Bean> queryBean(String provider, BeanReference<Bean> query) throws DiException {
        log.trace("Querying bean from provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null) {
            log.error("Invalid bean provider: {}", provider);
            throw new DiException("Invalid bean provider " + provider);
        }
        Optional<Bean> result = beanProvider.query(query);
        log.debug("Bean obtained from provider {}: {}", provider, result);
        return result;
    }

    @Override
    public <Bean> Optional<Bean> queryBean(BeanReference<Bean> query) throws DiException {
        log.trace("Querying bean from all providers, query: {}", query);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Objects.requireNonNull(query, "Bean query cannot be null");
        for (IBeanProvider provider : this.beanProviders.values()) {
            Optional<Bean> bean = provider.query(query);
            if (bean.isPresent()) {
                log.debug("Bean found in provider {}: {}", provider, bean);
                return bean;
            }
        }
        log.debug("No bean found for query {}", query);
        return Optional.empty();
    }

    @Override
    public <Bean> List<Bean> queryBeans(Optional<String> provider, BeanReference<Bean> query) throws DiException {
        log.trace("Querying beans with Optional provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(query, "Bean query cannot be null");
        List<Bean> result = provider.isPresent()
                ? this.queryBeans(provider.get(), query)
                : this.queryBeans(query);
        log.debug("Beans query result: {} items", result.size());
        return result;
    }

    @Override
    public <Bean> List<Bean> queryBeans(BeanReference<Bean> query) throws DiException {
        log.trace("Querying beans from all providers, query: {}", query);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Objects.requireNonNull(query, "Bean query cannot be null");
        List<Bean> beans = new ArrayList<>();
        for (IBeanProvider provider : this.beanProviders.values()) {
            List<Bean> providerBeans = provider.queries(query);
            beans.addAll(providerBeans);
            log.debug("Found {} beans in provider {}", providerBeans.size(), provider);
        }
        log.debug("Total beans found: {}", beans.size());
        return beans;
    }

    @Override
    public <Bean> List<Bean> queryBeans(String provider, BeanReference<Bean> query) throws DiException {
        log.trace("Querying beans from provider: {}, query: {}", provider, query);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null) {
            log.error("Invalid bean provider: {}", provider);
            throw new DiException("Invalid bean provider " + provider);
        }
        List<Bean> result = beanProvider.queries(query);
        log.debug("Beans obtained from provider {}: {} items", provider, result.size());
        return result;
    }

    @Override
    public Optional<IBeanProvider> getBeanProvider(String name) {
        log.trace("Getting bean provider: {}", name);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Optional<IBeanProvider> result = Optional.ofNullable(this.beanProviders.get(name));
        log.debug("Bean provider found: {}", result);
        return result;
    }

    @Override
    public Optional<IPropertyProvider> getPropertyProvider(String name) {
        log.trace("Getting property provider: {}", name);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Optional<IPropertyProvider> result = Optional.ofNullable(this.propertyProviders.get(name));
        log.debug("Property provider found: {}", result);
        return result;
    }

    /**
     * Returns the live, mutable map of registered bean providers keyed by scope name.
     *
     * @return the backing bean-provider map
     */
    public Map<String, IBeanProvider> beanProviders() {
        log.trace("Accessing beanProviders map");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        return this.beanProviders;
    }

    /**
     * Returns the live, mutable map of registered property providers keyed by scope name.
     *
     * @return the backing property-provider map
     */
    public Map<String, IPropertyProvider> propertyProviders() {
        log.trace("Accessing propertyProviders map");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        return this.propertyProviders;
    }

    /**
     * Returns the live, mutable list of registered child-context factories.
     *
     * @return the backing child-context-factory list
     */
    public List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories() {
        log.trace("Accessing childContextFactories list");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        return this.childContextFactories;
    }

    @Override
    public void registerChildContextFactory(IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.trace("Registering child context factory: {}", factory);
        wrapLifecycle(this::ensureInitialized, IClass.getClass(DiException.class));
        synchronized (this.mutex) {
            Objects.requireNonNull(factory, "Factory cannot be null");
            if (childContextFactories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
                childContextFactories.add(factory);
                log.debug("Child context factory registered: {}", factory);
            } else {
                log.warn("Child context factory already registered: {}", factory);
            }
        }
    }

    @Override
    public Resolved resolve(IClass<?> elementType, IAnnotatedElement element) throws DiException {
        log.trace("Resolving element: {}", element);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Resolved result = this.resolverDelegate.resolve(elementType, element);
        log.debug("Resolved element: {}", result);
        return result;
    }

    @Override
    public Set<Resolved> resolve(IExecutable method) throws DiException {
        log.trace("Resolving method: {}", method);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        Set<Resolved> result = this.resolverDelegate.resolve(method);
        log.debug("Resolved method: {} items", result.size());
        return result;
    }

    @Override
    public void addResolver(IClass<? extends Annotation> annotation, IElementResolver resolver) {
        log.trace("Adding resolver for annotation: {}", annotation);
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(DiException.class));
        synchronized (this.mutex) {
            this.resolverDelegate.addResolver(annotation, resolver);
            log.debug("Resolver added for annotation: {}", annotation);
        }
    }

    @Override
    public IInjectionContext copy() throws CopyException {
        log.trace("Copying InjectionContext");
        wrapLifecycle(this::ensureInitializedAndStarted, IClass.getClass(CopyException.class));
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

            log.debug("InjectionContext copied successfully");
            return copy;
        }
    }

    @Override
    public Set<IReflectionConfigurationEntryBuilder> reflectionUsage() {
                return this.getBeanProvider(Predefined.BeanProviders.garganttua.toString())
            .map(IBeanProvider::reflectionUsage)
            .orElse(Collections.emptySet());
    }

    @Override
    public <T> void addBean(String provider, BeanReference<T> reference, T bean, boolean autoDetect)
            throws DiException {
        IBeanProvider beanProvider = this.beanProviders.get(provider);
        if (beanProvider == null) {
            log.error("Invalid bean provider: {}", provider);
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

    // --- IBootstrapSummaryContributor implementation ---

    @Override
    public String getSummaryCategory() {
        return "Injection Context";
    }

    @Override
    public Map<String, String> getSummaryItems() {
        Map<String, String> items = new LinkedHashMap<>();
        items.put("Bean providers", String.valueOf(beanProviders.size()));
        items.put("Property providers", String.valueOf(propertyProviders.size()));
        items.put("Child context factories", String.valueOf(childContextFactories.size()));

        // Count total beans across all providers
        int totalBeans = beanProviders.values().stream()
                .mapToInt(provider -> {
                    try {
                        return provider.queries(new BeanReference<>(IClass.getClass(Object.class), Optional.empty(), Optional.empty(), Set.of())).size();
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();
        items.put("Total beans registered", String.valueOf(totalBeans));

        return items;
    }
}
