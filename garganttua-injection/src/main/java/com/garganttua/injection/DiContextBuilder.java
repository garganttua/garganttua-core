package com.garganttua.injection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IBeanProvider;
import com.garganttua.core.injection.IDiChildContextFactory;
import com.garganttua.core.injection.IDiContext;
import com.garganttua.core.injection.IPropertyProvider;
import com.garganttua.core.injection.context.dsl.IBeanProviderBuilder;
import com.garganttua.core.injection.context.dsl.IDiContextBuilder;
import com.garganttua.core.injection.context.dsl.IPropertyProviderBuilder;
import com.garganttua.injection.beans.Predefined;

public class DiContextBuilder implements IDiContextBuilder {

    private final Set<String> packages = new HashSet<>();
    private final Map<String, IBeanProviderBuilder> beanProviders = new HashMap<>();
    private final Map<String, IPropertyProviderBuilder> propertyProviders = new HashMap<>();
    private final List<IDiChildContextFactory<IDiContext>> childContextFactories = new ArrayList<>();

    public static DiContextBuilder builder() throws DslException{
        return new DiContextBuilder();
    }

    DiContextBuilder() throws DslException {
        this.beanProviders.put(Predefined.BeanProviders.garganttua.toString(), new BeanProviderBuilder(this).autoDetect(true));
        this.propertyProviders.put(Predefined.PropertyProviders.garganttua.toString(),
                new PropertyProviderBuilder(this));
    }

    @Override
    public IDiContextBuilder childContextFactory(IDiChildContextFactory<IDiContext> factory) {
        Objects.requireNonNull(factory, "ChildContextFactory cannot be null");
        if (childContextFactories.stream().noneMatch(f -> f.getClass().equals(factory.getClass()))) {
            childContextFactories.add(factory);
        }
        return this;
    }

    @Override
    public IDiContext build() throws DslException {
        if (beanProviders.isEmpty() && propertyProviders.isEmpty()) {
            throw new DslException("At least one BeanProvider or PropertyProvider must be provided");
        }

        return new DiContext(
                this.buildBeanProviders(),
                this.buildPropertyProviders(),
                Collections.unmodifiableList(new ArrayList<>(childContextFactories)));
    }

    private Map<String, IBeanProvider> buildBeanProviders()
            throws DslException {
        return this.beanProviders.entrySet().stream()
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

    private Map<String, IPropertyProvider> buildPropertyProviders()
            throws DslException {
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

}
