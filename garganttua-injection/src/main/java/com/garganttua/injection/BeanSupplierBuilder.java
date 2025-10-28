package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.beans.BeanDefinition;
import com.garganttua.injection.beans.BeanStrategy;

public class BeanSupplierBuilder<Bean> implements IBeanSupplierBuilder<Bean> {

    private String name = null;
    private String provider = null;
    private Class<Bean> type;
    private BeanStrategy strategy;
    private Class<? extends Annotation> qualifier;

    public BeanSupplierBuilder(Class<Bean> type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    @Override
    public Class<Bean> getObjectClass() {
        return type;
    }

    @Override
    public IBeanSupplier<Bean> build() throws DslException {
        if (type == null) {
            throw new DslException("Bean type must be provided");
        }
        Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
        if (this.qualifier != null) {
            qualifiers.add(this.qualifier);
        }
        return new BeanSupplier<Bean>(Optional.ofNullable(this.provider), BeanDefinition.example(this.type, Optional.ofNullable(this.strategy), Optional.ofNullable(this.name), qualifiers));
    }

    @Override
    public IBeanSupplierBuilder<Bean> name(String name) {
        this.name = Objects.requireNonNull(name, "Bean name cannot be null");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> provider(String provider) {
        this.provider = Objects.requireNonNull(provider, "Bean provider cannot be null");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) {
        this.qualifier = Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        return this;
    }

}
