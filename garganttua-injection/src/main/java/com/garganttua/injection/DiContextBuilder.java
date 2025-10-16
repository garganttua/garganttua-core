package com.garganttua.injection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.spec.IBeanScope;
import com.garganttua.injection.spec.IDiChildContextFactory;
import com.garganttua.injection.spec.IDiContext;
import com.garganttua.injection.spec.IDiContextBuilder;
import com.garganttua.injection.spec.IPropertyScope;

public class DiContextBuilder implements IDiContextBuilder {

    private final List<IBeanScope> beanScopes = new ArrayList<>();
    private final List<IPropertyScope> propertyScopes = new ArrayList<>();
    private final List<IDiChildContextFactory<? extends IDiContext>> childContextFactories = new ArrayList<>();

    @Override
    public IDiContextBuilder beanScope(IBeanScope scope) {
        Objects.requireNonNull(scope, "BeanScope cannot be null");

        boolean exists = beanScopes.stream()
                .anyMatch(s -> s.getName().equals(scope.getName()));
        if (!exists) {
            beanScopes.add(scope);
        }
        return this;
    }

    @Override
    public IDiContextBuilder propertyScope(IPropertyScope scope) {
        Objects.requireNonNull(scope, "PropertyScope cannot be null");

        boolean exists = propertyScopes.stream()
                .anyMatch(s -> s.getName().equals(scope.getName()));
        if (!exists) {
            propertyScopes.add(scope);
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
        if (beanScopes.isEmpty() && propertyScopes.isEmpty()) {
            throw new DslException("At least one BeanScope or PropertyScope must be provided");
        }

        return new DiContext(
                Collections.unmodifiableList(new ArrayList<>(beanScopes)),
                Collections.unmodifiableList(new ArrayList<>(propertyScopes)),
                Collections.unmodifiableList(new ArrayList<>(childContextFactories)));

    }
}
