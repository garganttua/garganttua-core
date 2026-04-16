package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.annotations.BeanProviderAnnotation;
import com.garganttua.core.injection.annotations.ChildContext;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.annotations.PropertyProviderAnnotation;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.beans.resolver.PrototypeElementResolver;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;
import com.garganttua.core.injection.context.properties.resolver.PropertyElementResolver;
import com.garganttua.core.injection.context.resolver.FixedElementResolver;
import com.garganttua.core.injection.context.resolver.NullElementResolver;
import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.reflection.annotations.ReflectedBuilder;
import com.garganttua.core.reflection.IClass;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bootstrap
@ReflectedBuilder
public class InjectionContextBuilder extends AbstractAutomaticDependentBuilder<IInjectionContextBuilder, IInjectionContext>
        implements IInjectionContextBuilder {

    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_BUILT_IN = "built-in";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    private final Set<String> packages = new HashSet<>();

    // Bean providers: built-in (P1) + manual (P0)
    private final Map<String, IBeanProviderBuilder> manualBeanProviders = new HashMap<>();
    private final Map<String, IBeanProviderBuilder> builtInBeanProviders = new HashMap<>();
    private final MultiSourceCollector<String, IBeanProviderBuilder> beanProviderCollector;

    // Property providers: built-in (P1) + manual (P0)
    private final Map<String, IPropertyProviderBuilder> manualPropertyProviders = new HashMap<>();
    private final Map<String, IPropertyProviderBuilder> builtInPropertyProviders = new HashMap<>();
    private final MultiSourceCollector<String, IPropertyProviderBuilder> propertyProviderCollector;

    // Child context factories: manual (P0) + auto-detected (P1)
    private final Map<String, IInjectionChildContextFactory<? extends IInjectionContext>> manualChildContextFactories = new HashMap<>();
    private final Map<String, IInjectionChildContextFactory<? extends IInjectionContext>> autoDetectedChildContextFactories = new HashMap<>();
    private final MultiSourceCollector<String, IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactoryCollector;

    // Qualifiers: manual (P0) + auto-detected (P1)
    private final Map<String, IClass<? extends Annotation>> manualQualifiers = new HashMap<>();
    private final Map<String, IClass<? extends Annotation>> autoDetectedQualifiers = new HashMap<>();
    private final MultiSourceCollector<String, IClass<? extends Annotation>> qualifierCollector;

    private IInjectableElementResolverBuilder resolvers;
    private Set<IBuilderObserver<IInjectionContextBuilder, IInjectionContext>> observers = new HashSet<>();

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

    public static IInjectionContextBuilder builder() throws DslException {
        log.atTrace().log("Entering InjectionContextBuilder.builder()");
        IInjectionContextBuilder builder = new InjectionContextBuilder();
        log.atTrace().log("Exiting InjectionContextBuilder.builder()");
        return builder;
    }

    private IReflection reflection;
    private IObservableBuilder<?, ?> reflectionBuilderRef;

    public InjectionContextBuilder() throws DslException {
        super(Set.of(DependencySpec.require(IClass.getClass(IReflectionBuilder.class), DependencyPhase.BOTH)));
        log.atTrace().log("Entering InjectionContextBuilder constructor");

        // Initialize collectors
        this.beanProviderCollector = new MultiSourceCollector<>();
        beanProviderCollector.source(mapSupplier(manualBeanProviders), 0, SOURCE_MANUAL);
        beanProviderCollector.source(mapSupplier(builtInBeanProviders), 1, SOURCE_BUILT_IN);

        this.propertyProviderCollector = new MultiSourceCollector<>();
        propertyProviderCollector.source(mapSupplier(manualPropertyProviders), 0, SOURCE_MANUAL);
        propertyProviderCollector.source(mapSupplier(builtInPropertyProviders), 1, SOURCE_BUILT_IN);

        this.childContextFactoryCollector = new MultiSourceCollector<>();
        childContextFactoryCollector.source(mapSupplier(manualChildContextFactories), 0, SOURCE_MANUAL);
        childContextFactoryCollector.source(mapSupplier(autoDetectedChildContextFactories), 1, SOURCE_AUTO_DETECTED);

        this.qualifierCollector = new MultiSourceCollector<>();
        qualifierCollector.source(mapSupplier(manualQualifiers), 0, SOURCE_MANUAL);
        qualifierCollector.source(mapSupplier(autoDetectedQualifiers), 1, SOURCE_AUTO_DETECTED);

        // Built-in providers
        this.builtInBeanProviders.put(Predefined.BeanProviders.garganttua.toString(),
                new BeanProviderBuilder(this).autoDetect(false));
        this.builtInPropertyProviders.put(Predefined.PropertyProviders.garganttua.toString(),
                new PropertyProviderBuilder(this).autoDetect(false));
        this.resolvers = new InjectableElementResolverBuilder(this);
        this.withPackage("com.garganttua.core.injection");
        log.atDebug().log("Initialized default bean and property providers and resolver");
        log.atTrace().log("Exiting InjectionContextBuilder constructor");
    }

    private Map<String, IBeanProviderBuilder> getAllBeanProviders() {
        return this.beanProviderCollector.build();
    }

    private Map<String, IPropertyProviderBuilder> getAllPropertyProviders() {
        return this.propertyProviderCollector.build();
    }

    @Override
    public IInjectionContextBuilder autoDetect(boolean b) throws DslException {
        getAllBeanProviders().values().forEach(bp -> bp.autoDetect(b));
        getAllPropertyProviders().values().forEach(pp -> pp.autoDetect(b));
        this.resolvers.autoDetect(b);
        return super.autoDetect(b);
    }

    @Override
    public IInjectionContextBuilder childContextFactory(
            IInjectionChildContextFactory<? extends IInjectionContext> factory) {
        log.atTrace().log("Entering childContextFactory(factory={})", factory);
        Objects.requireNonNull(factory, "ChildContextFactory cannot be null");
        this.manualChildContextFactories.put(factory.getClass().getName(), factory);
        log.atDebug().log("Added new child context factory: {}", factory);
        if (this.built != null) {
            this.built.registerChildContextFactory(factory);
            log.atDebug().log("Registered child context factory to built context: {}", factory);
        }
        log.atTrace().log("Exiting childContextFactory");
        return this;
    }

    private Map<String, IBeanProvider> buildBeanProviders(IInjectableElementResolver resolvers) {
        log.atTrace().log("Entering buildBeanProviders(resolvers={})", resolvers);
        Set<IClass<? extends Annotation>> allQualifiers = new HashSet<>(this.qualifierCollector.build().values());
        Map<String, IBeanProviderBuilder> allBeanProviders = getAllBeanProviders();
        Map<String, IBeanProvider> result = allBeanProviders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {

                            IBeanProviderBuilder provider = entry.getValue();
                            if (provider instanceof BeanProviderBuilder bpb) {
                                bpb.setQualifierAnnotations(allQualifiers);
                                if (this.reflectionBuilderRef != null) {
                                    bpb.provide(this.reflectionBuilderRef);
                                }
                                bpb.provide(this.resolvers);
                                log.atDebug().log("Configured BeanProviderBuilder for scope: {}", entry.getKey());
                            }
                            return provider.build();

                        }));
        log.atTrace().log("Exiting buildBeanProviders with result size: {}", result.size());
        return result;
    }

    private Map<String, IPropertyProvider> buildPropertyProviders() {
        log.atTrace().log("Entering buildPropertyProviders()");
        Map<String, IPropertyProvider> result = getAllPropertyProviders().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().build()));
        log.atTrace().log("Exiting buildPropertyProviders with result size: {}", result.size());
        return result;
    }

    @Override
    public IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider) {
        log.atTrace().log("Entering beanProvider(scope={}, provider={})", scope, provider);
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(provider, "BeanProvider cannot be null");
        provider.setUp(this);
        provider.autoDetect(isAutoDetected());
        manualBeanProviders.put(scope, provider);
        provider.withPackages(this.packages.stream().toArray(String[]::new));
        log.atDebug().log("Added bean provider for scope: {}", scope);
        log.atTrace().log("Exiting beanProvider");
        return provider;
    }

    @Override
    public IBeanProviderBuilder beanProvider(String scope) {
        log.atTrace().log("Entering beanProvider(scope={})", scope);
        Objects.requireNonNull(scope, "Scope cannot be null");
        IBeanProviderBuilder provider = getAllBeanProviders().get(scope);
        log.atTrace().log("Exiting beanProvider with provider={}", provider);
        return provider;
    }

    @Override
    public IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider) {
        log.atTrace().log("Entering propertyProvider(scope={}, provider={})", scope, provider);
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(provider, "PropertyProvider cannot be null");
        provider.setUp(this);
        manualPropertyProviders.put(scope, provider);
        log.atDebug().log("Added property provider for scope: {}", scope);
        log.atTrace().log("Exiting propertyProvider");
        return provider;
    }

    @Override
    public IPropertyProviderBuilder propertyProvider(String scope) {
        log.atTrace().log("Entering propertyProvider(scope={})", scope);
        Objects.requireNonNull(scope, "Scope cannot be null");
        IPropertyProviderBuilder provider = getAllPropertyProviders().get(scope);
        log.atTrace().log("Exiting propertyProvider with provider={}", provider);
        return provider;
    }

    @Override
    public IInjectionContextBuilder withPackages(String[] packageNames) {
        log.atTrace().log("Entering withPackages(packageNames={})", (Object) packageNames);
        this.packages.addAll(Set.of(packageNames));
        getAllBeanProviders().values().forEach(p -> p.withPackages(packageNames));
        this.resolvers.withPackages(packageNames);
        log.atDebug().log("Added packages: {}", Arrays.toString(packageNames));
        log.atTrace().log("Exiting withPackages");
        return this;
    }

    @Override
    public IInjectionContextBuilder withPackage(String packageName) {
        log.atTrace().log("Entering withPackage(packageName={})", packageName);
        this.packages.add(packageName);
        getAllBeanProviders().values().forEach(p -> p.withPackage(packageName));
        this.resolvers.withPackage(packageName);
        log.atDebug().log("Added package: {}", packageName);
        log.atTrace().log("Exiting withPackage");
        return this;
    }

    @Override
    public IInjectableElementResolverBuilder resolvers() {
        log.atTrace().log("Entering resolvers()");
        log.atTrace().log("Exiting resolvers with resolver={}", this.resolvers);
        return this.resolvers;
    }

    @Override
    public IInjectionContextBuilder withQualifier(IClass<? extends Annotation> qualifier) {
        log.atTrace().log("Entering withQualifier(qualifier={})", qualifier);
        Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        this.manualQualifiers.put(qualifier.getName(), qualifier);
        log.atDebug().log("Added qualifier: {}", qualifier);
        log.atTrace().log("Exiting withQualifier");
        return this;
    }

    @Override
    protected IInjectionContext doBuild() throws DslException {
        log.atTrace().log("Entering doBuild()");
        Map<String, IBeanProviderBuilder> allBeanProviders = getAllBeanProviders();
        Map<String, IPropertyProviderBuilder> allPropertyProviders = getAllPropertyProviders();

        if (allBeanProviders.isEmpty() && allPropertyProviders.isEmpty()) {
            log.atError().log("No BeanProvider or PropertyProvider defined. Throwing DslException.");
            throw new DslException("At least one BeanProvider and PropertyProvider must be provided");
        }

        Set<IClass<? extends Annotation>> allQualifiers = new HashSet<>(this.qualifierCollector.build().values());

        InjectionContextBuilder.setBuiltInResolvers(this.resolvers, allQualifiers, this.autoDetect.booleanValue());
        log.atDebug().log("Set built-in resolvers");

        allQualifiers.forEach(qualifier -> {
            this.resolvers.withResolver(qualifier, new SingletonElementResolver(Set.of()));
            log.atDebug().log("Added resolver for qualifier: {}", qualifier);
        });

        IInjectableElementResolver builtResolvers = this.resolvers.build();
        log.atDebug().log("Built IInjectableElementResolver");

        List<IInjectionChildContextFactory<? extends IInjectionContext>> allChildContextFactories =
                new ArrayList<>(this.childContextFactoryCollector.build().values());

        IInjectionContext built = InjectionContext.master(
                builtResolvers,
                this.buildBeanProviders(builtResolvers),
                this.buildPropertyProviders(),
                allChildContextFactories);

        log.atDebug().log("Constructed IInjectionContext master instance");
        this.notifyObserver(built);
        log.atTrace().log("Exiting doBuild()");
        return built;
    }

    private void notifyObserver(IInjectionContext built) {
        log.atTrace().log("Entering notifyObserver(built={})", built);
        this.observers.parallelStream().forEach(observer -> {
            observer.handle(built);
            log.atDebug().log("Notified observer: {}", observer);
        });
        log.atTrace().log("Exiting notifyObserver");
    }

    @SuppressWarnings("unchecked")
    public static void setBuiltInResolvers(IInjectableElementResolverBuilder resolvers,
            Set<IClass<? extends Annotation>> qualifiers, boolean autoDetect) {

        log.atTrace().log("Entering setBuiltInResolvers(resolvers={}, qualifiers={})", resolvers, qualifiers);
        resolvers.withResolver((IClass<? extends Annotation>) IClass.getClass(Singleton.class), new SingletonElementResolver(qualifiers))
                .withResolver((IClass<? extends Annotation>) IClass.getClass(Inject.class), new SingletonElementResolver(qualifiers))
                .withResolver((IClass<? extends Annotation>) IClass.getClass(Prototype.class), new PrototypeElementResolver(qualifiers));
        if (!autoDetect) {
            resolvers.withResolver((IClass<? extends Annotation>) IClass.getClass(Property.class), new PropertyElementResolver())
                    .withResolver((IClass<? extends Annotation>) IClass.getClass(Null.class), new NullElementResolver())
                    .withResolver((IClass<? extends Annotation>) IClass.getClass(Fixed.class), new FixedElementResolver());
        }
        log.atTrace().log("Exiting setBuiltInResolvers");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("doAutoDetection() — no-op, scanning deferred to doAutoDetectionWithDependency()");
    }

    @Override
    public String[] getPackages() {
        log.atTrace().log("Entering getPackages()");
        String[] pkgs = this.packages.toArray(new String[0]);
        log.atTrace().log("Exiting getPackages() with packages={}", Arrays.toString(pkgs));
        return pkgs;
    }

    @Override
    public IInjectionContextBuilder observer(IBuilderObserver<IInjectionContextBuilder, IInjectionContext> observer) {
        log.atTrace().log("Entering observer(observer={})", observer);
        Objects.requireNonNull(observer, "Observer cannot be null");

        this.observers.add(observer);
        log.atDebug().log("Added observer: {}", observer);

        // If context is already built, notify the observer immediately
        if (this.built != null) {
            observer.handle(this.built);
            log.atDebug().log("Context already built, immediately notified observer: {}", observer);
        }

        log.atTrace().log("Exiting observer");
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        if (dependency instanceof IReflection reflection) {
            log.atDebug().log("Received IReflection dependency, starting auto-detection");
            this.reflection = reflection;

            IClass<? extends Annotation> qualifierAnnotation = (IClass<? extends Annotation>) reflection.getClass(Qualifier.class);
            IClass<? extends Annotation> childContextAnnotation = (IClass<? extends Annotation>) reflection.getClass(ChildContext.class);
            IClass<?> childContextFactoryInterface = reflection.getClass(IInjectionChildContextFactory.class);

            // Auto-detect qualifiers
            this.packages.stream()
                    .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, qualifierAnnotation).stream()
                            .filter(clazz -> clazz.isAnnotationPresent(qualifierAnnotation)))
                    .map(clazz -> (IClass<? extends Annotation>) clazz)
                    .forEach(q -> this.autoDetectedQualifiers.put(q.getName(), q));
            log.atDebug().log("Auto-detected qualifiers: {}", this.autoDetectedQualifiers);

            // Auto-detect @ChildContext annotated classes
            this.packages.stream()
                    .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, childContextAnnotation).stream())
                    .forEach(factoryClass -> {
                        try {
                            if (childContextFactoryInterface.isAssignableFrom(factoryClass)) {
                                IInjectionChildContextFactory<? extends IInjectionContext> factory =
                                        (IInjectionChildContextFactory<? extends IInjectionContext>) reflection.newInstance(factoryClass);
                                this.autoDetectedChildContextFactories.put(factoryClass.getName(), factory);
                                log.atDebug().log("Auto-registered child context factory: {}", factoryClass.getName());
                            } else {
                                log.atWarn().log(
                                        "Class {} annotated with @ChildContext but does not implement IInjectionChildContextFactory",
                                        factoryClass.getName());
                            }
                        } catch (Exception e) {
                            log.atError().log("Failed to instantiate child context factory {}: {}",
                                    factoryClass.getName(), e.getMessage(), e);
                            throw new DslException("Failed to auto-detect child context factory: " + factoryClass.getName(), e);
                        }
                    });

            // Auto-detect @BeanProviderAnnotation annotated classes
            IClass<? extends Annotation> beanProviderAnnotation = (IClass<? extends Annotation>) reflection.getClass(BeanProviderAnnotation.class);
            IClass<?> beanProviderBuilderInterface = reflection.getClass(IBeanProviderBuilder.class);

            this.packages.stream()
                    .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, beanProviderAnnotation).stream())
                    .forEach(providerClass -> {
                        try {
                            if (beanProviderBuilderInterface.isAssignableFrom(providerClass)) {
                                BeanProviderAnnotation anno = ((Class<?>) providerClass.getType()).getAnnotation(BeanProviderAnnotation.class);
                                String scope = anno.value();
                                IBeanProviderBuilder provider = (IBeanProviderBuilder) reflection.newInstance(providerClass, this);
                                this.beanProvider(scope, provider);
                                log.atInfo().log("Auto-registered bean provider '{}' from {}", scope, providerClass.getName());
                            } else {
                                log.atWarn().log("Class {} annotated with @BeanProviderAnnotation but does not implement IBeanProviderBuilder",
                                        providerClass.getName());
                            }
                        } catch (Exception e) {
                            log.atError().log("Failed to instantiate bean provider {}: {}", providerClass.getName(), e.getMessage(), e);
                        }
                    });

            // Auto-detect @PropertyProviderAnnotation annotated classes
            IClass<? extends Annotation> propertyProviderAnnotation = (IClass<? extends Annotation>) reflection.getClass(PropertyProviderAnnotation.class);
            IClass<?> propertyProviderBuilderInterface = reflection.getClass(IPropertyProviderBuilder.class);

            this.packages.stream()
                    .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, propertyProviderAnnotation).stream())
                    .forEach(providerClass -> {
                        try {
                            if (propertyProviderBuilderInterface.isAssignableFrom(providerClass)) {
                                PropertyProviderAnnotation anno = ((Class<?>) providerClass.getType()).getAnnotation(PropertyProviderAnnotation.class);
                                String scope = anno.value();
                                IPropertyProviderBuilder provider = (IPropertyProviderBuilder) reflection.newInstance(providerClass, this);
                                this.propertyProvider(scope, provider);
                                log.atInfo().log("Auto-registered property provider '{}' from {}", scope, providerClass.getName());
                            } else {
                                log.atWarn().log("Class {} annotated with @PropertyProviderAnnotation but does not implement IPropertyProviderBuilder",
                                        providerClass.getName());
                            }
                        } catch (Exception e) {
                            log.atError().log("Failed to instantiate property provider {}: {}", providerClass.getName(), e.getMessage(), e);
                        }
                    });

            // Pass IReflection to sub-builders for their own auto-detection
            getAllBeanProviders().values().forEach(bp -> {
                if (bp instanceof BeanProviderBuilder bpb) {
                    bpb.setReflection(reflection);
                }
            });
            if (this.resolvers instanceof InjectableElementResolverBuilder ier) {
                ier.setReflection(reflection);
            }

            log.atDebug().log("Auto-detection with IReflection completed");
        }
    }

    @Override
    public IInjectionContextBuilder provide(IObservableBuilder<?, ?> dependency) throws DslException {
        if (dependency instanceof IReflectionBuilder) {
            this.reflectionBuilderRef = dependency;
        }
        return super.provide(dependency);
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // IReflection already handled in doAutoDetectionWithDependency
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build behavior needed
    }
}