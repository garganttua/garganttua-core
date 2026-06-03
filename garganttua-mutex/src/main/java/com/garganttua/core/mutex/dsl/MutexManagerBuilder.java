package com.garganttua.core.mutex.dsl;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.mutex.IMutex;
import com.garganttua.core.mutex.IMutexFactory;
import com.garganttua.core.mutex.IMutexManager;
import com.garganttua.core.mutex.MutexManager;
import com.garganttua.core.mutex.annotations.Mutex;
import com.garganttua.core.mutex.annotations.MutexFactory;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Fluent builder that assembles an {@link IMutexManager} from factories collected
 * across multiple sources: manual registrations, the injection context, and
 * package reflection scanning.
 *
 * <p>
 * Sources are merged through a {@link MultiSourceCollector} with a fixed precedence
 * (manual &gt; context &gt; reflection). On build, factories discovered outside the
 * injection context are registered as beans and the resulting manager is itself
 * exposed as an {@link IMutexManager} bean. The builder also wires a
 * {@link MutexResolver} into the injection context for {@code @Mutex}-annotated
 * elements.
 * </p>
 *
 * <p>
 * Discoverable via the bootstrap SPI ({@link MutexManagerBuilderFactory}); declared
 * {@link Bootstrap @Bootstrap} so {@code Bootstrap.autoDetect(true)} picks it up.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManagerBuilder
 * @see MutexManager
 */
@Bootstrap
@Reflected
@ConfigurableBuilder("mutex")
public class MutexManagerBuilder extends AbstractAutomaticDependentBuilder<IMutexManagerBuilder, IMutexManager>
        implements IMutexManagerBuilder {
    private static final Logger log = Logger.getLogger(MutexManagerBuilder.class);

    private static final String SOURCE_CONTEXT = "context";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_REFLECTION = "reflection";

    private final Set<String> packages = new HashSet<>();
    private final Map<IClass<? extends IMutex>, IMutexFactory> manualFactories = new HashMap<>();
    private final Map<IClass<? extends IMutex>, IMutexFactory> contextFactories = new HashMap<>();
    private final Map<IClass<? extends IMutex>, IMutexFactory> reflexionFactories = new HashMap<>();

    private final MultiSourceCollector<IClass<? extends IMutex>, IMutexFactory> collector;

    private MutexManagerBuilder() {
        super(Set.of(DependencySpec.use(IClass.getClass(IInjectionContextBuilder.class))));

        this.collector = new MultiSourceCollector<>();
        collector.source(mapSupplier(manualFactories), 0, SOURCE_MANUAL);
        collector.source(mapSupplier(contextFactories), 1, SOURCE_CONTEXT);
        collector.source(mapSupplier(reflexionFactories), 2, SOURCE_REFLECTION);

        log.debug("MutexManagerBuilder initialized");
    }

    @SuppressWarnings("unchecked")
    private static <K, V> ISupplier<Map<K, V>> mapSupplier(Map<K, V> map) {
        return new ISupplier<>() {
            @Override
            public Optional<Map<K, V>> supply() throws SupplyException {
                return Optional.of(map);
            }

            @Override
            public Type getSuppliedType() {
                return Map.class;
            }

            @Override
            public IClass<Map<K, V>> getSuppliedClass() {
                return (IClass<Map<K, V>>) (IClass<?>) IClass.getClass(Map.class);
            }
        };
    }

    @Override
    public IMutexManagerBuilder withPackage(String packageName) {
        log.debug("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IMutexManagerBuilder withPackages(String[] packageNames) {
        log.debug("Adding {} packages", packageNames.length);
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    public IMutexManagerBuilder withFactory(IClass<? extends IMutex> type, IMutexFactory factory) {
        log.trace("Entering withFactory({}, {})", type.getSimpleName(), factory.getClass().getSimpleName());

        Objects.requireNonNull(type, "Mutex type cannot be null");
        Objects.requireNonNull(factory, "Mutex factory cannot be null");

        this.manualFactories.put(type, factory);

        log.debug("Registered factory {} for mutex type {}",
                factory.getClass().getSimpleName(), type.getSimpleName());

        log.trace("Exiting withFactory()");
        return this;
    }

    @Override
    protected IMutexManager doBuild() throws DslException {
        log.trace("Entering doBuild() method");
        Map<IClass<? extends IMutex>, IMutexFactory> factories = this.collector.build();
        log.debug("Building MutexManager with {} registered factories", factories.size());
        IMutexManager manager = new MutexManager(factories);

        log.trace("Exiting doBuild() method");
        return manager;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("Entering doAutoDetection() method");
        log.debug("Auto-detecting mutex factories in {} packages", packages.size());

        for (String packageName : packages) {
            try {
                collectFactoriesFromReflexion(packageName);
            } catch (DslException e) {
                log.error("Failed to scan package: {}", packageName, e);
                throw new IllegalStateException("Failed to scan package: " + packageName, e);
            }
        }

        log.trace("Exiting doAutoDetection() method");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.trace("Entering doAutoDetectionWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            log.debug("Collecting factories from InjectionContext");

            BeanReference<IMutexFactory> beanRef = new BeanReference<>(
                    IClass.getClass(IMutexFactory.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.empty(),
                    Set.of(IClass.getClass(MutexFactory.class)));

            context.getBeanProvider(Predefined.BeanProviders.garganttua.toString())
                    .ifPresent(provider -> provider.queries(beanRef).forEach(
                            f -> {
                                MutexFactory ann = f.getClass().getAnnotation(MutexFactory.class);
                                if (ann != null) {
                                    this.contextFactories.put(IClass.getClass(ann.type()), f);
                                }
                            }));
        }

        log.trace("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.trace("Entering doPreBuildWithDependency() with dependency: {}", dependency);
        log.trace("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.trace("Entering doPostBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            String providerName = Predefined.BeanProviders.garganttua.toString();

            final Map<IClass<? extends IMutex>, IMutexFactory> factoriesToBeAddedToContext = this
                    .collector.buildExcludingSourceItems(Set.of(SOURCE_CONTEXT));

            factoriesToBeAddedToContext.forEach((type, factory) -> {
                BeanReference<IMutexFactory> beanRef = new BeanReference<>(
                        IClass.getClass(IMutexFactory.class),
                        Optional.of(BeanStrategy.singleton),
                        Optional.empty(),
                        Set.of(IClass.getClass(MutexFactory.class)));
                context.addBean(providerName, beanRef, factory);
            });

            log.debug("Registering IMutexManager as bean in InjectionContext");
            BeanReference<IMutexManager> managerBeanRef = new BeanReference<>(
                    IClass.getClass(IMutexManager.class),
                    Optional.of(BeanStrategy.singleton),
                    Optional.empty(),
                    Set.of());
            context.addBean(providerName, managerBeanRef, this.built);
            log.debug("IMutexManager successfully registered as bean");
        }

        log.trace("Exiting doPostBuildWithDependency() method");
    }

    private void collectFactoriesFromReflexion(String packageName) throws DslException {
        log.debug("Scanning package: {}", packageName);
        IReflection reflection = IClass.getReflection();
        IClass<MutexFactory> mutexFactoryAnnotation = IClass.getClass(MutexFactory.class);
        for (IClass<?> factoryClass : reflection.getClassesWithAnnotation(packageName, mutexFactoryAnnotation)) {
            MutexFactory annotation = factoryClass.getAnnotation(mutexFactoryAnnotation);
            IClass<? extends IMutex> mutexType = IClass.getClass(annotation.type());
            try {
                IMutexFactory factory = (IMutexFactory) reflection.newInstance(factoryClass);
                this.reflexionFactories.putIfAbsent(mutexType, factory);
            } catch (ReflectionException e) {
                throw new DslException("Failed to instantiate mutex factory: " + factoryClass.getName(), e);
            }
        }
    }

    @Override
    public IMutexManagerBuilder provide(IObservableBuilder<?, ?> dependency) {
        if (dependency instanceof IInjectionContextBuilder injectionContext) {
            this.addResolverToInjectionContext(injectionContext);
        }
        return super.provide(dependency);
    }

    private void addResolverToInjectionContext(IInjectionContextBuilder context) {
        log.trace("Entering addResolverToInjectionContext() method");
        Objects.requireNonNull(context, "Context builder cannot be null");
        context.resolvers().withResolver(IClass.getClass(Mutex.class), new MutexResolver());
        log.debug("Context builder configured");
        log.trace("Exiting addResolverToInjectionContext() method");
    }

    public static IMutexManagerBuilder builder() {
        log.trace("Entering builder() with no parameters");
        IMutexManagerBuilder result = new MutexManagerBuilder();
        log.trace("Exiting builder() with no parameters");
        return result;
    }

}
