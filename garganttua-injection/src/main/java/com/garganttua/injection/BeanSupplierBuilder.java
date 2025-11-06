package com.garganttua.injection;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.dsl.IBeanSupplierBuilder;

public class BeanSupplierBuilder<Bean> implements IBeanSupplierBuilder<Bean> {

    private String name = null;
    private String provider = null;
    private Class<Bean> type;
    private BeanStrategy strategy;
    private Class<? extends Annotation> qualifier;

    public BeanSupplierBuilder(Class<Bean> type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
    }

    public BeanSupplierBuilder(Optional<String> provider, BeanDefinition<Bean> example) {
        Objects.requireNonNull(example, "Example cannot be null");

        if (provider != null && provider.isPresent()) {
            initFromExample(example);
            this.provider = provider.get();
        } else {
            initFromExample(example);
        }
    }

    public BeanSupplierBuilder(BeanDefinition<Bean> example) {
        Objects.requireNonNull(example, "Example cannot be null");
        initFromExample(example);
    }

    public BeanSupplierBuilder(String provider, BeanDefinition<Bean> example) {
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(example, "Example cannot be null");

        initFromExample(example);
        this.provider = provider;
    }

    private void initFromExample(BeanDefinition<Bean> example) {
        this.type = example.type();
        this.name = example.name().orElse(null);
        this.strategy = example.strategy().orElse(BeanStrategy.singleton);
        if (!example.qualifiers().isEmpty()) {
            this.qualifier = example.qualifiers().iterator().next();
        }
    }

    @Override
    public Class<Bean> getSuppliedType() {
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
        return new BeanSupplier<Bean>(Optional.ofNullable(this.provider), BeanDefinition.example(this.type,
                Optional.ofNullable(this.strategy), Optional.ofNullable(this.name), qualifiers));
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

    @Override
    public Set<Class<?>> getDependencies() {
        return Set.of();
    }

    @Override
    public boolean isContextual() {
        return false;
    }

}
