package com.garganttua.injection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.IBeanProvider;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.injection.spec.IPropertyProvider;

public class DiContextBuilder implements IDiContextBuilder {

    private final List<IBeanProvider> beanProviders = new ArrayList<>();
    private final List<IPropertyProvider> propertyProviders = new ArrayList<>();
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories = new ArrayList<>();

    @Override
    public IDiContextBuilder beanProvider(IBeanProvider provider) {
        Objects.requireNonNull(provider, "BeanProvider cannot be null");

        boolean exists = beanProviders.stream()
                .anyMatch(s -> s.getName().equals(provider.getName()));
        if (!exists) {
            beanProviders.add(provider);
        }
        return this;
    }

    @Override
    public IDiContextBuilder propertyProvider(IPropertyProvider provider) {
        Objects.requireNonNull(provider, "PropertyProvider cannot be null");

        boolean exists = propertyProviders.stream()
                .anyMatch(s -> s.getName().equals(provider.getName()));
        if (!exists) {
            propertyProviders.add(provider);
        }
        return this;
    }

    @Override
    public IDiContextBuilder childContextFactory(IDiChildContextFactory<? extends IDiContext> factory) {
        Objects.requireNonNull(factory, "ChildContextFactory cannot be null");

        boolean exists = childContextFactories.stream()
                .anyMatch(f -> f.getClass().equals(factory.getClass()));

        if (!exists) {
            childContextFactories.add(factory);
        }
        return this;
    }

    @Override
    public IDiContext build() throws DslException {
        if (beanProviders.isEmpty() && propertyProviders.isEmpty()) {
            throw new DslException("At least one BeanProvider and PropertyProvider must be provided");
        }

        return new DiContext(
                Collections.unmodifiableList(new ArrayList<>(beanProviders)),
                Collections.unmodifiableList(new ArrayList<>(propertyProviders)),
                Collections.unmodifiableList(new ArrayList<>(childContextFactories)));

    }
}
