package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.Predefined;
import com.garganttua.core.injection.context.beans.resolver.PrototypeElementResolver;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;
import com.garganttua.core.injection.context.properties.resolver.PropertyElementResolver;
import com.garganttua.core.injection.context.resolver.FixedElementResolver;
import com.garganttua.core.injection.context.resolver.NullElementResolver;
import com.garganttua.core.nativve.annotations.NativeConfigurationBuilder;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@NativeConfigurationBuilder
public class DiContextBuilder extends AbstractAutomaticBuilder<IDiContextBuilder, IDiContext>
        implements IDiContextBuilder {

    private final Set<String> packages = new HashSet<>();
    private final Map<String, IBeanProviderBuilder> beanProviders = new HashMap<>();
    private final Map<String, IPropertyProviderBuilder> propertyProviders = new HashMap<>();
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories = new ArrayList<>();
    private IInjectableElementResolverBuilder resolvers;
    private Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
    private Set<IContextBuilderObserver> observers = new HashSet<>();

    public static IDiContextBuilder builder() throws DslException {
        log.atTrace().log("Entering DiContextBuilder.builder()");
        IDiContextBuilder builder = new DiContextBuilder();
        log.atTrace().log("Exiting DiContextBuilder.builder()");
        return builder;
    }

    public DiContextBuilder() throws DslException {
        log.atTrace().log("Entering DiContextBuilder constructor");
        this.beanProviders.put(Predefined.BeanProviders.garganttua.toString(),
                new BeanProviderBuilder(this).autoDetect(true));
        this.propertyProviders.put(Predefined.PropertyProviders.garganttua.toString(),
                new PropertyProviderBuilder(this).autoDetect(false));

        this.resolvers = new InjectableElementResolverBuilder(this);
        log.atDebug().log("Initialized default bean and property providers and resolver");
        log.atTrace().log("Exiting DiContextBuilder constructor");
    }

    @Override
    public IDiContextBuilder childContextFactory(IDiChildContextFactory<? extends IDiContext> factory) {
        log.atTrace().log("Entering childContextFactory(factory={})", factory);
        Objects.requireNonNull(factory, "ChildContextFactory cannot be null");
        if (childContextFactories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
            childContextFactories.add(factory);
            log.atDebug().log("Added new child context factory: {}", factory);
        }
        if (this.built != null) {
            this.built.registerChildContextFactory(factory);
            log.atInfo().log("Registered child context factory to built context: {}", factory);
        }
        log.atTrace().log("Exiting childContextFactory");
        return this;
    }

    private Map<String, IBeanProvider> buildBeanProviders(IInjectableElementResolver resolvers) {
        log.atTrace().log("Entering buildBeanProviders(resolvers={})", resolvers);
        Map<String, IBeanProvider> result = this.beanProviders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                IBeanProviderBuilder provider = entry.getValue();
                                if (provider instanceof BeanProviderBuilder bpb) {
                                    bpb.setQualifierAnnotations(this.qualifiers);
                                    bpb.setResolver(resolvers);
                                    log.atDebug().log("Configured BeanProviderBuilder for scope: {}", entry.getKey());
                                }
                                return provider.build();
                            } catch (DslException e) {
                                log.atError().log("Error building BeanProvider for scope: {}",
                                        entry.getKey());
                                throw new RuntimeException("Error building BeanProvider for scope: " + entry.getKey(),
                                        e);
                            }
                        }));
        log.atTrace().log("Exiting buildBeanProviders with result size: {}", result.size());
        return result;
    }

    private Map<String, IPropertyProvider> buildPropertyProviders() {
        log.atTrace().log("Entering buildPropertyProviders()");
        Map<String, IPropertyProvider> result = this.propertyProviders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                return entry.getValue().build();
                            } catch (DslException e) {
                                log.atError().log("Error building PropertyProvider for scope: {}",
                                        entry.getKey());
                                throw new RuntimeException("Error building BeanProvider for scope: " + entry.getKey(),
                                        e);
                            }
                        }));
        log.atTrace().log("Exiting buildPropertyProviders with result size: {}", result.size());
        return result;
    }

    @Override
    public IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider) {
        log.atTrace().log("Entering beanProvider(scope={}, provider={})", scope, provider);
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(provider, "BeanProvider cannot be null");
        provider.setUp(this);
        beanProviders.put(scope, provider);
        provider.withPackages(this.packages.stream().toArray(String[]::new));
        log.atInfo().log("Added bean provider for scope: {}", scope);
        log.atTrace().log("Exiting beanProvider");
        return provider;
    }

    @Override
    public IBeanProviderBuilder beanProvider(String scope) {
        log.atTrace().log("Entering beanProvider(scope={})", scope);
        Objects.requireNonNull(scope, "Scope cannot be null");
        IBeanProviderBuilder provider = beanProviders.get(scope);
        log.atTrace().log("Exiting beanProvider with provider={}", provider);
        return provider;
    }

    @Override
    public IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider) {
        log.atTrace().log("Entering propertyProvider(scope={}, provider={})", scope, provider);
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(provider, "PropertyProvider cannot be null");
        provider.setUp(this);
        propertyProviders.put(scope, provider);
        log.atInfo().log("Added property provider for scope: {}", scope);
        log.atTrace().log("Exiting propertyProvider");
        return provider;
    }

    @Override
    public IPropertyProviderBuilder propertyProvider(String scope) {
        log.atTrace().log("Entering propertyProvider(scope={})", scope);
        Objects.requireNonNull(scope, "Scope cannot be null");
        IPropertyProviderBuilder provider = propertyProviders.get(scope);
        log.atTrace().log("Exiting propertyProvider with provider={}", provider);
        return provider;
    }

    @Override
    public IDiContextBuilder withPackages(String[] packageNames) {
        log.atTrace().log("Entering withPackages(packageNames={})", (Object) packageNames);
        this.packages.addAll(Set.of(packageNames));
        this.beanProviders.values().stream().forEach(p -> p.withPackages(packageNames));
        log.atInfo().log("Added packages: {}", Arrays.toString(packageNames));
        log.atTrace().log("Exiting withPackages");
        return this;
    }

    @Override
    public IDiContextBuilder withPackage(String packageName) {
        log.atTrace().log("Entering withPackage(packageName={})", packageName);
        this.packages.add(packageName);
        this.beanProviders.values().stream().forEach(p -> p.withPackage(packageName));
        log.atInfo().log("Added package: {}", packageName);
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
    public IDiContextBuilder withQualifier(Class<? extends Annotation> qualifier) {
        log.atTrace().log("Entering withQualifier(qualifier={})", qualifier);
        this.qualifiers.add(Objects.requireNonNull(qualifier, "Qualifier cannot be null"));
        log.atInfo().log("Added qualifier: {}", qualifier);
        log.atTrace().log("Exiting withQualifier");
        return this;
    }

    @Override
    protected IDiContext doBuild() throws DslException {
        log.atTrace().log("Entering doBuild()");
        if (beanProviders.isEmpty() && propertyProviders.isEmpty()) {
            log.atError().log("No BeanProvider or PropertyProvider defined. Throwing DslException.");
            throw new DslException("At least one BeanProvider and PropertyProvider must be provided");
        }

        DiContextBuilder.setBuiltInResolvers(this.resolvers, this.qualifiers);
        log.atDebug().log("Set built-in resolvers");

        this.qualifiers.forEach(qualifier -> {
            this.resolvers.withResolver(qualifier, new SingletonElementResolver(Set.of()));
            log.atDebug().log("Added resolver for qualifier: {}", qualifier);
        });

        IInjectableElementResolver builtResolvers = this.resolvers.build();
        log.atDebug().log("Built IInjectableElementResolver");

        IDiContext built = DiContext.master(
                builtResolvers,
                this.buildBeanProviders(builtResolvers),
                this.buildPropertyProviders(),
                new ArrayList<>(childContextFactories));

        log.atInfo().log("Constructed IDiContext master instance");
        this.notifyObserver(built);
        log.atTrace().log("Exiting doBuild()");
        return built;
    }

    private void notifyObserver(IDiContext built) {
        log.atTrace().log("Entering notifyObserver(built={})", built);
        this.observers.parallelStream().forEach(observer -> {
            observer.handle(built);
            log.atDebug().log("Notified observer: {}", observer);
        });
        log.atTrace().log("Exiting notifyObserver");
    }

    public static void setBuiltInResolvers(IInjectableElementResolverBuilder resolvers,
            Set<Class<? extends Annotation>> qualifiers) {

        log.atTrace().log("Entering setBuiltInResolvers(resolvers={}, qualifiers={})", resolvers, qualifiers);
        resolvers.withResolver(Singleton.class, new SingletonElementResolver(qualifiers))
                .withResolver(Inject.class, new SingletonElementResolver(qualifiers))
                .withResolver(Prototype.class, new PrototypeElementResolver(qualifiers))
                .withResolver(Property.class, new PropertyElementResolver())
                .withResolver(Null.class, new NullElementResolver())
                .withResolver(Fixed.class, new FixedElementResolver());
        log.atTrace().log("Exiting setBuiltInResolvers");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection()");

        // Auto-detect @Qualifier annotations
        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper.getClassesWithAnnotation(package_, Qualifier.class)
                        .stream()
                        .filter(clazz -> clazz.getAnnotation(Qualifier.class) != null))
                .map(clazz -> (Class<? extends Annotation>) clazz)
                .forEach(this.qualifiers::add);
        log.atInfo().log("Auto-detected qualifiers: {}", this.qualifiers);

        // Auto-detect @Resolver classes
        this.autoDetectResolvers();

        log.atTrace().log("Exiting doAutoDetection");
    }

    /**
     * Auto-detects and registers resolver classes annotated with @Resolver.
     * This method scans all configured packages for classes with the @Resolver annotation,
     * instantiates them, and registers them for the annotations they handle.
     *
     * @throws DslException if a resolver cannot be instantiated or registered
     */
    private void autoDetectResolvers() throws DslException {
        log.atTrace().log("Entering autoDetectResolvers()");

        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper
                        .getClassesWithAnnotation(package_, com.garganttua.core.injection.annotations.Resolver.class)
                        .stream())
                .forEach(resolverClass -> {
                    try {
                        log.atDebug().log("Found resolver class: {}", resolverClass.getName());

                        // Get the @Resolver annotation
                        com.garganttua.core.injection.annotations.Resolver resolverAnnotation =
                                resolverClass.getAnnotation(com.garganttua.core.injection.annotations.Resolver.class);

                        if (resolverAnnotation == null) {
                            log.atWarn().log("Resolver class {} has no @Resolver annotation", resolverClass.getName());
                            return;
                        }

                        // Get the annotations this resolver handles
                        Class<? extends Annotation>[] handledAnnotations = resolverAnnotation.annotations();

                        if (handledAnnotations == null || handledAnnotations.length == 0) {
                            log.atWarn().log("Resolver class {} handles no annotations", resolverClass.getName());
                            return;
                        }

                        // Instantiate the resolver
                        IElementResolver resolverInstance = instantiateResolver(resolverClass);

                        // Register the resolver for each annotation it handles
                        for (Class<? extends Annotation> annotation : handledAnnotations) {
                            this.resolvers.withResolver(annotation, resolverInstance);
                            log.atInfo().log("Auto-registered resolver {} for annotation {}",
                                    resolverClass.getSimpleName(), annotation.getSimpleName());
                        }

                    } catch (Exception e) {
                        log.atError().log("Failed to auto-detect resolver: {}", resolverClass.getName(), e);
                        throw new RuntimeException("Failed to auto-detect resolver: " + resolverClass.getName(), e);
                    }
                });

        log.atTrace().log("Exiting autoDetectResolvers");
    }

    /**
     * Instantiates a resolver class using reflection.
     * Tries multiple constructor strategies:
     * 1. No-arg constructor
     * 2. Constructor with Set<Class<? extends Annotation>> parameter (for qualifiers)
     * 3. Constructor with IBeanProvider parameter
     *
     * @param resolverClass the resolver class to instantiate
     * @return an instance of the resolver
     * @throws DslException if the resolver cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    private IElementResolver instantiateResolver(Class<?> resolverClass) throws DslException {
        log.atTrace().log("Entering instantiateResolver for class: {}", resolverClass.getName());

        try {
            // Strategy 1: Try no-arg constructor
            try {
                IElementResolver instance = (IElementResolver) resolverClass.getDeclaredConstructor().newInstance();
                log.atDebug().log("Instantiated resolver {} using no-arg constructor",
                        resolverClass.getSimpleName());
                return instance;
            } catch (NoSuchMethodException e) {
                log.atDebug().log("No no-arg constructor found for {}", resolverClass.getSimpleName());
            }

            // Strategy 2: Try constructor with Set<Class<? extends Annotation>> (qualifiers)
            try {
                IElementResolver instance = (IElementResolver) resolverClass
                        .getDeclaredConstructor(Set.class)
                        .newInstance(this.qualifiers);
                log.atDebug().log("Instantiated resolver {} using constructor with qualifiers",
                        resolverClass.getSimpleName());
                return instance;
            } catch (NoSuchMethodException e) {
                log.atDebug().log("No Set constructor found for {}", resolverClass.getSimpleName());
            }

            // If we get here, no suitable constructor was found
            throw new DslException("No suitable constructor found for resolver class: " + resolverClass.getName() +
                    ". Expected either no-arg constructor or constructor with Set<Class<? extends Annotation>> parameter.");

        } catch (DslException e) {
            throw e;
        } catch (Exception e) {
            log.atError().log("Failed to instantiate resolver: {}", resolverClass.getName(), e);
            throw new DslException("Failed to instantiate resolver: " + resolverClass.getName(), e);
        } finally {
            log.atTrace().log("Exiting instantiateResolver");
        }
    }

    @Override
    public IDiContextBuilder observer(IContextBuilderObserver observer) {
        log.atTrace().log("Entering observer(observer={})", observer);
        this.observers.add(Objects.requireNonNull(observer, "Observer cannot be null"));
        if (this.built != null) {
            observer.handle(this.built);
            log.atDebug().log("Notified observer of already built context");
        }
        log.atTrace().log("Exiting observer");
        return this;
    }

    @Override
    public String[] getPackages() {
        log.atTrace().log("Entering getPackages()");
        String[] pkgs = this.packages.toArray(new String[0]);
        log.atTrace().log("Exiting getPackages() with packages={}", Arrays.toString(pkgs));
        return pkgs;
    }
}