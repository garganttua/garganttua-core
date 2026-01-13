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
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IInjectionChildContextFactory;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.annotations.ChildContext;
import com.garganttua.core.injection.annotations.Fixed;
import com.garganttua.core.injection.annotations.Null;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.injection.context.InjectionContext;
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
public class InjectionContextBuilder extends AbstractAutomaticBuilder<IInjectionContextBuilder, IInjectionContext>
        implements IInjectionContextBuilder {

    private final Set<String> packages = new HashSet<>();
    private final Map<String, IBeanProviderBuilder> beanProviders = new HashMap<>();
    private final Map<String, IPropertyProviderBuilder> propertyProviders = new HashMap<>();
    private final List<IInjectionChildContextFactory<? extends IInjectionContext>> childContextFactories = new ArrayList<>();
    private IInjectableElementResolverBuilder resolvers;
    private Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
    private Set<IBuilderObserver<IInjectionContextBuilder, IInjectionContext>> observers = new HashSet<>();

    public static IInjectionContextBuilder builder() throws DslException {
        log.atTrace().log("Entering InjectionContextBuilder.builder()");
        IInjectionContextBuilder builder = new InjectionContextBuilder();
        log.atTrace().log("Exiting InjectionContextBuilder.builder()");
        return builder;
    }

    public InjectionContextBuilder() throws DslException {
        log.atTrace().log("Entering InjectionContextBuilder constructor");
        this.beanProviders.put(Predefined.BeanProviders.garganttua.toString(),
                new BeanProviderBuilder(this).autoDetect(true));
        this.propertyProviders.put(Predefined.PropertyProviders.garganttua.toString(),
                new PropertyProviderBuilder(this).autoDetect(false));

        this.resolvers = new InjectableElementResolverBuilder(this);
        log.atDebug().log("Initialized default bean and property providers and resolver");
        log.atTrace().log("Exiting InjectionContextBuilder constructor");
    }

    @Override
    public IInjectionContextBuilder childContextFactory(
            IInjectionChildContextFactory<? extends IInjectionContext> factory) {
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

                            IBeanProviderBuilder provider = entry.getValue();
                            if (provider instanceof BeanProviderBuilder bpb) {
                                bpb.setQualifierAnnotations(this.qualifiers);
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
        Map<String, IPropertyProvider> result = this.propertyProviders.entrySet().stream()
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
    public IInjectionContextBuilder withPackages(String[] packageNames) {
        log.atTrace().log("Entering withPackages(packageNames={})", (Object) packageNames);
        this.packages.addAll(Set.of(packageNames));
        this.beanProviders.values().stream().forEach(p -> p.withPackages(packageNames));
        log.atInfo().log("Added packages: {}", Arrays.toString(packageNames));
        log.atTrace().log("Exiting withPackages");
        return this;
    }

    @Override
    public IInjectionContextBuilder withPackage(String packageName) {
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
    public IInjectionContextBuilder withQualifier(Class<? extends Annotation> qualifier) {
        log.atTrace().log("Entering withQualifier(qualifier={})", qualifier);
        this.qualifiers.add(Objects.requireNonNull(qualifier, "Qualifier cannot be null"));
        log.atInfo().log("Added qualifier: {}", qualifier);
        log.atTrace().log("Exiting withQualifier");
        return this;
    }

    @Override
    protected IInjectionContext doBuild() throws DslException {
        log.atTrace().log("Entering doBuild()");
        if (beanProviders.isEmpty() && propertyProviders.isEmpty()) {
            log.atError().log("No BeanProvider or PropertyProvider defined. Throwing DslException.");
            throw new DslException("At least one BeanProvider and PropertyProvider must be provided");
        }

        InjectionContextBuilder.setBuiltInResolvers(this.resolvers, this.qualifiers);
        log.atDebug().log("Set built-in resolvers");

        this.qualifiers.forEach(qualifier -> {
            this.resolvers.withResolver(qualifier, new SingletonElementResolver(Set.of()));
            log.atDebug().log("Added resolver for qualifier: {}", qualifier);
        });

        IInjectableElementResolver builtResolvers = this.resolvers.build();
        log.atDebug().log("Built IInjectableElementResolver");

        IInjectionContext built = InjectionContext.master(
                builtResolvers,
                this.buildBeanProviders(builtResolvers),
                this.buildPropertyProviders(),
                new ArrayList<>(childContextFactories));

        log.atInfo().log("Constructed IInjectionContext master instance");
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

        // Auto-detect qualifiers
        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper.getClassesWithAnnotation(package_, Qualifier.class)
                        .stream()
                        .filter(clazz -> clazz.getAnnotation(Qualifier.class) != null))
                .map(clazz -> (Class<? extends Annotation>) clazz)
                .forEach(this.qualifiers::add);
        log.atInfo().log("Auto-detected qualifiers: {}", this.qualifiers);

        // Auto-detect @Resolver annotated classes
        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper.getClassesWithAnnotation(package_, Resolver.class).stream())
                .forEach(resolverClass -> {
                    try {
                        Resolver annotation = resolverClass.getAnnotation(Resolver.class);
                        if (annotation != null && IInjectableElementResolver.class.isAssignableFrom(resolverClass)) {
                            IInjectableElementResolver<?> resolverInstance =
                                (IInjectableElementResolver<?>) resolverClass.getDeclaredConstructor().newInstance();

                            for (Class<? extends Annotation> annotationType : annotation.annotations()) {
                                this.resolvers.withResolver(annotationType, resolverInstance);
                                log.atInfo().log("Auto-registered resolver {} for annotation {}",
                                    resolverClass.getSimpleName(), annotationType.getSimpleName());
                            }
                        } else {
                            log.atWarn().log("Class {} annotated with @Resolver but does not implement IInjectableElementResolver",
                                resolverClass.getName());
                        }
                    } catch (Exception e) {
                        log.atError().log("Failed to instantiate resolver {}: {}", resolverClass.getName(), e.getMessage(), e);
                        throw new DslException("Failed to auto-detect resolver: " + resolverClass.getName(), e);
                    }
                });

        // Auto-detect @ChildContext annotated classes
        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper.getClassesWithAnnotation(package_, ChildContext.class).stream())
                .forEach(factoryClass -> {
                    try {
                        if (IInjectionChildContextFactory.class.isAssignableFrom(factoryClass)) {
                            IInjectionChildContextFactory<? extends IInjectionContext> factory =
                                (IInjectionChildContextFactory<? extends IInjectionContext>)
                                    factoryClass.getDeclaredConstructor().newInstance();

                            this.childContextFactory(factory);
                            log.atInfo().log("Auto-registered child context factory: {}",
                                factoryClass.getSimpleName());
                        } else {
                            log.atWarn().log("Class {} annotated with @ChildContext but does not implement IInjectionChildContextFactory",
                                factoryClass.getName());
                        }
                    } catch (Exception e) {
                        log.atError().log("Failed to instantiate child context factory {}: {}",
                            factoryClass.getName(), e.getMessage(), e);
                        throw new DslException("Failed to auto-detect child context factory: " + factoryClass.getName(), e);
                    }
                });

        log.atTrace().log("Exiting doAutoDetection");
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
            log.atInfo().log("Context already built, immediately notified observer: {}", observer);
        }

        log.atTrace().log("Exiting observer");
        return this;
    }
}