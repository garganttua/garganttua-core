package com.garganttua.core.mutex.dsl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.ContextReadinessBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
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
public class MutexManagerBuilder extends AbstractAutomaticBuilder<IMutexManagerBuilder, IMutexManager>
        implements IMutexManagerBuilder {

    private Map<Class<? extends IMutex>, IMutexFactory> factories = new HashMap<>();
    private ContextReadinessBuilder<IMutexManagerBuilder> readinessBuilder;
    private Set<String> packages = new HashSet<>();

    private MutexManagerBuilder(Optional<IDiContextBuilder> contextBuilder) {
        super();
        this.readinessBuilder = new ContextReadinessBuilder<>(contextBuilder, this);
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

        this.factories.put(type, factory);

        log.atInfo().log("Registered factory {} for mutex type {}",
                factory.getClass().getSimpleName(), type.getSimpleName());

        log.atTrace().log("Exiting withFactory()");
        return this;
    }

    @Override
    protected IMutexManager doBuild() throws DslException {
        log.atTrace().log("Entering doBuild() method");

        this.readinessBuilder.requireBuildAuthorization();

        log.atInfo().log("Building MutexManager with {} registered factories", factories.size());

        IMutexManager manager = new MutexManager(this.factories);

        log.atTrace().log("Exiting doBuild() method");
        return manager;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection() method");

        if (shouldSkipAutoDetection()) {
            log.atWarn().log("Auto-detection requested but no DI context available, skipping auto-detection");
            return;
        }

        log.atInfo().log("Auto-detecting mutex factories in {} packages", packages.size());

        for (String packageName : packages) {
            scanPackageForFactories(packageName);
        }

        log.atTrace().log("Exiting doAutoDetection() method");
    }

    /**
     * Determines if auto-detection should be skipped.
     *
     * @return true if auto-detection should be skipped, false otherwise
     */
    private boolean shouldSkipAutoDetection() {
        return this.readinessBuilder.canBuild();
    }

    /**
     * Scans a package for classes annotated with @MutexFactory and registers them.
     *
     * @param packageName the package to scan
     * @throws DslException if factory registration fails
     */
    private void scanPackageForFactories(String packageName) throws DslException {
        log.atDebug().log("Scanning package: {}", packageName);

        for (Class<?> factoryClass : ObjectReflectionHelper.getClassesWithAnnotation(packageName, MutexFactory.class)) {
            registerDiscoveredFactory(factoryClass);
        }
    }

    /**
     * Registers a discovered factory class.
     *
     * @param factoryClass the factory class to register
     * @throws DslException if registration fails
     */
    private void registerDiscoveredFactory(Class<?> factoryClass) throws DslException {
        MutexFactory annotation = factoryClass.getAnnotation(MutexFactory.class);
        if (annotation == null) {
            log.atWarn().log("Class {} found but missing @MutexFactory annotation", factoryClass.getSimpleName());
            return;
        }

        log.atDebug().log("Registering factory: {} for mutex type: {}",
                factoryClass.getSimpleName(), annotation.type().getSimpleName());

        if (this.readinessBuilder.hasContext()) {
            registerFactoryInDiContext(factoryClass, annotation);
        } else {
            registerFactoryDirectly(factoryClass, annotation);
        }
    }

    /**
     * Registers a factory in the DI context.
     *
     * @param factoryClass the factory class
     * @param annotation the @MutexFactory annotation
     * @throws DslException if registration fails
     */
    @SuppressWarnings("unchecked")
    private void registerFactoryInDiContext(Class<?> factoryClass, MutexFactory annotation) throws DslException {
        Class<IMutexFactory> mutexFactoryClass = (Class<IMutexFactory>) (Class<?>) factoryClass;

        BeanReference<IMutexFactory> beanRef = new BeanReference<>(
                mutexFactoryClass,
                Optional.of(BeanStrategy.singleton),
                Optional.empty(),
                Set.of(MutexFactory.class)
        );

        this.readinessBuilder.getContext().addBean(Predefined.BeanProviders.garganttua.toString(), beanRef);

        IMutexFactory factory = retrieveFactoryFromContext(beanRef, factoryClass);
        withFactory(annotation.type(), factory);
    }

    /**
     * Retrieves a factory instance from the DI context.
     *
     * @param beanRef the bean reference
     * @param factoryClass the factory class
     * @return the factory instance
     * @throws DslException if factory cannot be retrieved
     */
    private IMutexFactory retrieveFactoryFromContext(BeanReference<IMutexFactory> beanRef, Class<?> factoryClass)
            throws DslException {
        return this.readinessBuilder.getContext()
                .getBeanProvider(Predefined.BeanProviders.garganttua.toString())
                .flatMap(provider -> provider.query(beanRef))
                .orElseThrow(() -> new DslException(
                        "Mutex Factory bean of type " + factoryClass.getSimpleName() +
                        " should have been created in DI Context but was not found"
                ));
    }

    /**
     * Registers a factory directly by instantiating it.
     *
     * @param factoryClass the factory class
     * @param annotation the @MutexFactory annotation
     */
    private void registerFactoryDirectly(Class<?> factoryClass, MutexFactory annotation) {
        log.atDebug().log("Creating factory instance directly: {}", factoryClass.getSimpleName());

        IMutexFactory factory = (IMutexFactory) ObjectReflectionHelper.instanciateNewObject(factoryClass);
        withFactory(annotation.type(), factory);
    }

    @Override
    public IMutexManagerBuilder context(IDiContextBuilder context) {
        log.atTrace().log("Entering context() method");

        Objects.requireNonNull(context, "Context builder cannot be null");

        this.readinessBuilder.setContextBuilder(context);
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
    public static IMutexManagerBuilder builder(IDiContextBuilder contextBuilder) {
        log.atTrace().log("Entering builder() with IDiContextBuilder parameter");
        IMutexManagerBuilder result = new MutexManagerBuilder(Optional.ofNullable(contextBuilder));
        log.atTrace().log("Exiting builder() with IDiContextBuilder parameter");
        return result;
    }

    /**
     * Creates a new builder instance with an optional dependency injection context.
     *
     * @param contextBuilder the optional context builder to use
     * @return a new MutexManagerBuilder instance
     */
    public static IMutexManagerBuilder builder(Optional<IDiContextBuilder> contextBuilder) {
        log.atTrace().log("Entering builder() with Optional<IDiContextBuilder> parameter");
        IMutexManagerBuilder result = new MutexManagerBuilder(contextBuilder);
        log.atTrace().log("Exiting builder() with Optional<IDiContextBuilder> parameter");
        return result;
    }

}
