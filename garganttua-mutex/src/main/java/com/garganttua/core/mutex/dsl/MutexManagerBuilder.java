package com.garganttua.core.mutex.dsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
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
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;
import com.garganttua.core.supply.FixedSupplier;

import lombok.extern.slf4j.Slf4j;

/**
 * Builder for constructing {@link IMutexManager} instances with configurable
 * mutex factories.
 *
 * <p>
 * This builder allows configuring different mutex factories for different mutex
 * types,
 * enabling flexible mutex management strategies within an application.
 * </p>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * IMutexManager manager = MutexManagerBuilder.builder()
 *         .withFactory(InterruptibleLeaseMutex.class, new InterruptibleLeaseMutexFactory())
 *         .build();
 * }</pre>
 *
 * <h2>Auto-detection</h2>
 * <p>
 * When auto-detection is enabled, the builder will scan the dependency
 * injection context
 * for beans annotated with {@code @MutexFactory} and automatically register
 * them.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This builder is not thread-safe. Each instance should be used by a single
 * thread
 * during construction.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IMutexManager
 * @see IMutexFactory
 */
@Slf4j
public class MutexManagerBuilder extends AbstractAutomaticDependentBuilder<IMutexManagerBuilder, IMutexManager>
        implements IMutexManagerBuilder {

    private static final String SOURCE_CONTEXT = "context";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_REFLECTION = "reflection";

    private final Set<String> packages = new HashSet<>();
    private final Map<Class<? extends IMutex>, IMutexFactory> manualFactories = new HashMap<>();
    private final Map<Class<? extends IMutex>, IMutexFactory> contextFactories = new HashMap<>();
    private final Map<Class<? extends IMutex>, IMutexFactory> reflexionFactories = new HashMap<>();

    private final MultiSourceCollector<Class<? extends IMutex>, IMutexFactory> collector;

    private MutexManagerBuilder() {
        super(
                Set.of(IInjectionContextBuilder.class),
                Set.of());
                
        this.collector = new MultiSourceCollector<>();
        collector.source(new FixedSupplier<>(manualFactories), 0, SOURCE_MANUAL);
        collector.source(new FixedSupplier<>(contextFactories), 1, SOURCE_CONTEXT);
        collector.source(new FixedSupplier<>(reflexionFactories), 2, SOURCE_REFLECTION);
        
        log.atInfo().log("MutexManagerBuilder initialized");
    }

    @Override
    public IMutexManagerBuilder withPackage(String packageName) {
        log.atDebug().log("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IMutexManagerBuilder withPackages(String[] packageNames) {
        log.atDebug().log("Adding {} packages", packageNames.length);
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
    public IMutexManagerBuilder withFactory(Class<? extends IMutex> type, IMutexFactory factory) {
        log.atTrace().log("Entering withFactory({}, {})", type.getSimpleName(), factory.getClass().getSimpleName());

        Objects.requireNonNull(type, "Mutex type cannot be null");
        Objects.requireNonNull(factory, "Mutex factory cannot be null");

        this.manualFactories.put(type, factory);

        log.atInfo().log("Registered factory {} for mutex type {}",
                factory.getClass().getSimpleName(), type.getSimpleName());

        log.atTrace().log("Exiting withFactory()");
        return this;
    }

    @Override
    protected IMutexManager doBuild() throws DslException {
        log.atTrace().log("Entering doBuild() method");
        Map<Class<? extends IMutex>, IMutexFactory> factories = this.collector.build();
        log.atInfo().log("Building MutexManager with {} registered factories", factories.size());
        IMutexManager manager = new MutexManager(factories);

        log.atTrace().log("Exiting doBuild() method");
        return manager;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection() method");
        log.atInfo().log("Auto-detecting mutex factories in {} packages", packages.size());

        for (String packageName : packages) {
            try {
                collectFactoriesFromReflexion(packageName);
            } catch (DslException e) {
                log.atError().log("Failed to scan package: {}", packageName, e);
                throw new IllegalStateException("Failed to scan package: " + packageName, e);
            }
        }

        log.atTrace().log("Exiting doAutoDetection() method");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        log.atTrace().log("Entering doAutoDetectionWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            log.atDebug().log("Collecting factories from InjectionContext");

            BeanReference<IMutexFactory> beanRef = new BeanReference<>(
                    IMutexFactory.class,
                    Optional.of(BeanStrategy.singleton),
                    Optional.empty(),
                    Set.of(MutexFactory.class));

            context.getBeanProvider(Predefined.BeanProviders.garganttua.toString())
                    .ifPresent(provider -> provider.queries(beanRef).forEach(
                            f -> this.contextFactories.put(f.getClass().getAnnotation(MutexFactory.class).type(), f)));
        }

        log.atTrace().log("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPreBuildWithDependency() with dependency: {}", dependency);
        log.atTrace().log("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPostBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {

            final Map<Class<? extends IMutex>, IMutexFactory> factoriesToBeAddedToContext = this
                    .collector.buildExcludingSourceItems(Set.of(SOURCE_CONTEXT));

            factoriesToBeAddedToContext.forEach((type, factory) -> {
                BeanReference<IMutexFactory> beanRef = new BeanReference<>(
                        IMutexFactory.class,
                        Optional.of(BeanStrategy.singleton),
                        Optional.empty(),
                        Set.of(MutexFactory.class));

                context.getBeanProvider(Predefined.BeanProviders.garganttua.toString())
                        .ifPresent(provider -> provider.add(beanRef, factory));
            });
        }

        if (dependency instanceof IInjectionContext context) {
            log.atDebug().log("Registering IMutexManager as bean in InjectionContext");
            context.getBeanProvider(Predefined.BeanProviders.garganttua.toString())
                    .ifPresent(provider -> {
                        BeanReference<IMutexManager> beanRef = new BeanReference<>(
                                IMutexManager.class,
                                Optional.of(BeanStrategy.singleton),
                                Optional.empty(),
                                Set.of());
                        provider.add(beanRef, this.built);
                        log.atInfo().log("IMutexManager successfully registered as bean");
                    });
        }

        log.atTrace().log("Exiting doPostBuildWithDependency() method");
    }

    private void collectFactoriesFromReflexion(String packageName) {
        log.atDebug().log("Scanning package: {}", packageName);
        for (Class<?> factoryClass : ObjectReflectionHelper.getClassesWithAnnotation(packageName, MutexFactory.class)) {
            Class<? extends IMutex> mutexType = factoryClass.getAnnotation(MutexFactory.class).type();
            IMutexFactory factory = (IMutexFactory) ObjectReflectionHelper.instanciateNewObject(factoryClass);
            this.reflexionFactories.putIfAbsent(mutexType, factory);
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
        log.atTrace().log("Entering addResolverToInjectionContext() method");
        Objects.requireNonNull(context, "Context builder cannot be null");
        context.resolvers().withResolver(Mutex.class, new MutexResolver());
        log.atInfo().log("Context builder configured");
        log.atTrace().log("Exiting addResolverToInjectionContext() method");
    }

    /**
     * Creates a new builder instance without a dependency injection context.
     *
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder() {
        log.atTrace().log("Entering builder() with no parameters");
        IMutexManagerBuilder result = new MutexManagerBuilder();
        log.atTrace().log("Exiting builder() with no parameters");
        return result;
    }

}
