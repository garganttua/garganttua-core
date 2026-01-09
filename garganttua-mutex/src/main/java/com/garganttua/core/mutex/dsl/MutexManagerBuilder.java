package com.garganttua.core.mutex.dsl;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
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

    private MutexManagerBuilder(Optional<IInjectionContextBuilder> contextBuilder) {
        super(
            contextBuilder.isPresent() ? Set.of(IInjectionContextBuilder.class) : Set.of(),
            Set.of()
        );

        contextBuilder.ifPresent(this::provide);

        log.atInfo().log("MutexManagerBuilder initialized");
    }

    @Override
    public MutexManagerBuilder withPackage(String packageName) {
        log.atDebug().log("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public MutexManagerBuilder withPackages(String[] packageNames) {
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
        Map<Class<? extends IMutex>, IMutexFactory> factories = this.computeFactoriesForBuild();
        log.atInfo().log("Building MutexManager with {} registered factories", factories.size());
        IMutexManager manager = new MutexManager(factories);

        log.atTrace().log("Exiting doBuild() method");
        return manager;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection() method");

        // Synchronize packages from InjectionContextBuilder before scanning
        synchronizePackagesFromContext();

        log.atInfo().log("Auto-detecting mutex factories in {} packages", packages.size());

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
                    .ifPresent(provider ->
                        provider.queries(beanRef).forEach(f ->
                            this.contextFactories.put(f.getClass().getAnnotation(MutexFactory.class).type(), f)
                        )
                    );
        }

        // Phase 2: Scanner les packages par réflexion et découvrir les classes de factories
        for (String packageName : packages) {
            try {
                collectFactoriesFromReflexion(packageName);
            } catch (DslException e) {
                log.atError().log("Failed to scan package: {}", packageName, e);
                throw new IllegalStateException("Failed to scan package: " + packageName, e);
            }
        }

        log.atTrace().log("Exiting doAutoDetectionWithDependency() method");
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPreBuildWithDependency() with dependency: {}", dependency);

        if (dependency instanceof IInjectionContext context) {
            // Phase 3: Instancier et enregistrer les factories découvertes
            final Map<Class<? extends IMutex>, IMutexFactory> factoriesToBeAddedToContext =
                    this.computeFactoriesToBeAddedToContext();

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

        log.atTrace().log("Exiting doPreBuildWithDependency() method");
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        log.atTrace().log("Entering doPostBuildWithDependency() with dependency: {}", dependency);

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

    /**
     * Synchronizes packages from the InjectionContextBuilder to this builder's packages.
     * This ensures that packages declared in the DI context are also scanned for
     * mutex factories.
     */
    private void synchronizePackagesFromContext() {
        log.atTrace().log("Entering synchronizePackagesFromContext()");

        useDependencies.stream()
            .filter(dep -> dep.getDependency().equals(IInjectionContextBuilder.class))
            .findFirst()
            .ifPresent(dep -> dep.synchronizePackagesFromContext(contextPackages -> {
                int beforeSize = this.packages.size();
                this.packages.addAll(contextPackages);
                int addedCount = this.packages.size() - beforeSize;
                if (addedCount > 0) {
                    log.atDebug().log("Synchronized {} new packages from InjectionContextBuilder", addedCount);
                }
            }));

        log.atTrace().log("Exiting synchronizePackagesFromContext()");
    }

    private ISupplier<Map<Class<? extends IMutex>, IMutexFactory>> factorySupplier(
            Map<Class<? extends IMutex>, IMutexFactory> factories) {
        return new ISupplier<Map<Class<? extends IMutex>, IMutexFactory>>() {
            @Override
            public Optional<Map<Class<? extends IMutex>, IMutexFactory>> supply() throws SupplyException {
                return Optional.of(factories);
            }

            @Override
            public Type getSuppliedType() {
                throw new UnsupportedOperationException("Unimplemented method 'getSuppliedType'");
            }
        };
    }

    private Map<Class<? extends IMutex>, IMutexFactory> computeFactoriesToBeAddedToContext() {
        MultiSourceCollector<Class<? extends IMutex>, IMutexFactory> collector = new MultiSourceCollector<>();

        collector.source(factorySupplier(contextFactories), 0, SOURCE_CONTEXT);
        collector.source(factorySupplier(manualFactories), 1, SOURCE_MANUAL);
        collector.source(factorySupplier(reflexionFactories), 2, SOURCE_REFLECTION);

        return collector.buildExcludingSourceItems(Set.of(SOURCE_CONTEXT));
    }

    private Map<Class<? extends IMutex>, IMutexFactory> computeFactoriesForBuild() {
        MultiSourceCollector<Class<? extends IMutex>, IMutexFactory> collector = new MultiSourceCollector<>();

        collector.source(factorySupplier(contextFactories), 0, SOURCE_CONTEXT);
        collector.source(factorySupplier(manualFactories), 1, SOURCE_MANUAL);
        collector.source(factorySupplier(reflexionFactories), 2, SOURCE_REFLECTION);

        return collector.build();
    }

    @Override
    public IMutexManagerBuilder context(IInjectionContextBuilder context) {
        log.atTrace().log("Entering context() method");

        Objects.requireNonNull(context, "Context builder cannot be null");

        this.provide(context);
        context.resolvers().withResolver(Mutex.class, new MutexResolver());

        log.atInfo().log("Context builder configured");

        log.atTrace().log("Exiting context() method");
        return this;
    }

    /**
     * Creates a new builder instance without a dependency injection context.
     *
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder() {
        log.atTrace().log("Entering builder() with no parameters");
        IMutexManagerBuilder result = new MutexManagerBuilder(Optional.empty());
        log.atTrace().log("Exiting builder() with no parameters");
        return result;
    }

    /**
     * Creates a new builder instance with a dependency injection context.
     *
     * @param contextBuilder the context builder to use
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder(IInjectionContextBuilder contextBuilder) {
        log.atTrace().log("Entering builder() with IInjectionContextBuilder parameter");
        IMutexManagerBuilder result = new MutexManagerBuilder(Optional.ofNullable(contextBuilder));
        log.atTrace().log("Exiting builder() with IInjectionContextBuilder parameter");
        return result;
    }

    /**
     * Creates a new builder instance with an optional dependency injection context.
     *
     * @param contextBuilder the optional context builder to use
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder(Optional<IInjectionContextBuilder> contextBuilder) {
        log.atTrace().log("Entering builder() with Optional<IInjectionContextBuilder> parameter");
        IMutexManagerBuilder result = new MutexManagerBuilder(contextBuilder);
        log.atTrace().log("Exiting builder() with Optional<IInjectionContextBuilder> parameter");
        return result;
    }

}
