package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
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
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.annotations.Property;
import com.garganttua.core.injection.annotations.Prototype;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.Predefined;
import com.garganttua.core.injection.context.beans.resolver.PrototypeElementResolver;
import com.garganttua.core.injection.context.beans.resolver.SingletonElementResolver;
import com.garganttua.core.injection.context.properties.resolver.PropertyElementResolver;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

public class DiContextBuilder extends AbstractAutomaticBuilder<IDiContextBuilder, IDiContext>
        implements IDiContextBuilder {

    private final Set<String> packages = new HashSet<>();
    private final Map<String, IBeanProviderBuilder> beanProviders = new HashMap<>();
    private final Map<String, IPropertyProviderBuilder> propertyProviders = new HashMap<>();
    private final List<IDiChildContextFactory<IDiContext>> childContextFactories = new ArrayList<>();
    private IInjectableElementResolverBuilder resolvers;
    private Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
    private Set<IContextBuilderObserver> observers = new HashSet<>();

    public static DiContextBuilder builder() throws DslException {
        return new DiContextBuilder();
    }

    public DiContextBuilder() throws DslException {
        this.beanProviders.put(Predefined.BeanProviders.garganttua.toString(),
                new BeanProviderBuilder(this).autoDetect(true));
        this.propertyProviders.put(Predefined.PropertyProviders.garganttua.toString(),
                new PropertyProviderBuilder(this));

        this.resolvers = new InjectableElementResolverBuilder(this);
    }

    @Override
    public IDiContextBuilder childContextFactory(IDiChildContextFactory<IDiContext> factory) {
        Objects.requireNonNull(factory, "ChildContextFactory cannot be null");
        if (childContextFactories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
            childContextFactories.add(factory);
        }
        return this;
    }

    private Map<String, IBeanProvider> buildBeanProviders() {
        return this.beanProviders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                IBeanProviderBuilder provider = entry.getValue();
                                if (provider instanceof BeanProviderBuilder bpb) {
                                    bpb.setQualifierAnnotations(this.qualifiers);
                                    bpb.setResolver(this.resolvers.build());
                                }
                                return provider.build();
                            } catch (DslException e) {
                                throw new RuntimeException("Error building BeanProvider for scope: " + entry.getKey(),
                                        e);
                            }
                        }));
    }

    private Map<String, IPropertyProvider> buildPropertyProviders() {
        return this.propertyProviders.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            try {
                                return entry.getValue().build();
                            } catch (DslException e) {
                                throw new RuntimeException("Error building BeanProvider for scope: " + entry.getKey(),
                                        e);
                            }
                        }));
    }

    @Override
    public IBeanProviderBuilder beanProvider(String scope, IBeanProviderBuilder provider) {
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(provider, "BeanProvider cannot be null");
        provider.setUp(this);
        beanProviders.put(scope, provider);
        provider.withPackages(this.packages.stream().toArray(String[]::new));
        return provider;
    }

    @Override
    public IBeanProviderBuilder beanProvider(String scope) {
        Objects.requireNonNull(scope, "Scope cannot be null");
        return beanProviders.get(scope);
    }

    @Override
    public IPropertyProviderBuilder propertyProvider(String scope, IPropertyProviderBuilder provider) {
        Objects.requireNonNull(scope, "Scope cannot be null");
        Objects.requireNonNull(provider, "PropertyProvider cannot be null");
        provider.setUp(this);
        propertyProviders.put(scope, provider);
        return provider;
    }

    @Override
    public IPropertyProviderBuilder propertyProvider(String scope) {
        Objects.requireNonNull(scope, "Scope cannot be null");
        return propertyProviders.get(scope);
    }

    @Override
    public IDiContextBuilder withPackages(String[] packageNames) {
        this.packages.addAll(Set.of(packageNames));
        this.beanProviders.values().stream().forEach(p -> p.withPackages(packageNames));
        return this;
    }

    @Override
    public IDiContextBuilder withPackage(String packageName) {
        this.packages.add(packageName);
        this.beanProviders.values().stream().forEach(p -> p.withPackage(packageName));
        return this;
    }

    @Override
    public IInjectableElementResolverBuilder resolvers() {
        return this.resolvers;
    }

    @Override
    public IDiContextBuilder withQualifier(Class<? extends Annotation> qualifier) {
        this.qualifiers.add(Objects.requireNonNull(qualifier, "Qualifier cannot be null"));
        return this;
    }

    @Override
    protected IDiContext doBuild() throws DslException {
        if (beanProviders.isEmpty() && propertyProviders.isEmpty()) {
            throw new DslException("At least one BeanProvider and PropertyProvider must be provided");
        }

        DiContextBuilder.setBuiltInResolvers(this.resolvers, this.qualifiers);

        this.qualifiers.forEach(qualifier -> {
            this.resolvers.withResolver(qualifier, new SingletonElementResolver(Set.of()));
        });

        IDiContext built = new DiContext(
                this.buildBeanProviders(),
                this.buildPropertyProviders(),
                Collections.unmodifiableList(new ArrayList<>(childContextFactories)));

        this.notifyObserver(built);
        return built;
    }

    private void notifyObserver(IDiContext built) {
        this.observers.parallelStream().forEach(observer -> observer.handle(built));
    }

    public static void setBuiltInResolvers(IInjectableElementResolverBuilder resolvers,
            Set<Class<? extends Annotation>> qualifiers) {

        resolvers.withResolver(Singleton.class, new SingletonElementResolver(qualifiers))
                .withResolver(Inject.class, new SingletonElementResolver(qualifiers))
                .withResolver(Prototype.class, new PrototypeElementResolver(qualifiers))
                .withResolver(Property.class, new PropertyElementResolver());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAutoDetection() throws DslException {
        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper.getClassesWithAnnotation(package_, Qualifier.class)
                        .stream()
                        .filter(clazz -> clazz.getAnnotation(Qualifier.class) != null))
                .map(clazz -> (Class<? extends Annotation>) clazz)
                .forEach(this.qualifiers::add);
    }

    @Override
    public IDiContextBuilder observer(IContextBuilderObserver observer) {
        this.observers.add(Objects.requireNonNull(observer, "Observer cannot be null"));
        if (this.built != null) {
            Thread.ofVirtual().start(() -> observer.handle(this.built));
        }
        return this;
    }

}
