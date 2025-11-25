package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.beans.BeanSupplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanSupplierBuilder<Bean> implements IBeanSupplierBuilder<Bean> {

    private String name = null;
    private String provider = null;
    private Class<Bean> type;
    private BeanStrategy strategy;
    private Class<? extends Annotation> qualifier;

    public BeanSupplierBuilder(Class<Bean> type) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with type: {}", type);
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        log.atDebug().log("Type set to: {}", this.type.getSimpleName());
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    public BeanSupplierBuilder(Optional<String> provider, BeanDefinition<Bean> example) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with Optional provider: {} and example: {}",
                provider, example);
        Objects.requireNonNull(example, "Example cannot be null");
        log.atDebug().log("Example provided: {}", example);

        if (provider != null && provider.isPresent()) {
            initFromExample(example);
            this.provider = provider.get();
            log.atDebug().log("Provider set from Optional: {}", this.provider);
        } else {
            initFromExample(example);
        }
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    public BeanSupplierBuilder(BeanDefinition<Bean> example) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with example: {}", example);
        Objects.requireNonNull(example, "Example cannot be null");
        initFromExample(example);
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    public BeanSupplierBuilder(String provider, BeanDefinition<Bean> example) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with provider: {} and example: {}", provider,
                example);
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(example, "Example cannot be null");

        initFromExample(example);
        this.provider = provider;
        log.atDebug().log("Provider set to: {}", this.provider);
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    private void initFromExample(BeanDefinition<Bean> example) {
        log.atTrace().log("Initializing BeanSupplierBuilder from example: {}", example);
        this.type = example.type();
        this.name = example.name().orElse(null);
        this.strategy = example.strategy().orElse(BeanStrategy.singleton);
        if (!example.qualifiers().isEmpty()) {
            this.qualifier = example.qualifiers().iterator().next();
            log.atDebug().log("Qualifier set from example: {}", this.qualifier.getSimpleName());
        }
        log.atDebug().log("Initialization from example complete. Type: {}, Name: {}, Strategy: {}, Qualifier: {}",
                this.type.getSimpleName(), this.name, this.strategy, this.qualifier);
        log.atTrace().log("Exiting initFromExample method");
    }

    @Override
    public Class<Bean> getSuppliedType() {
        log.atTrace().log("Entering getSuppliedType() method");
        log.atTrace().log("Exiting getSuppliedType() method with type: {}", this.type);
        return type;
    }

    @Override
    public IBeanSupplier<Bean> build() throws DslException {
        log.atTrace().log("Entering build() method");
        if (type == null) {
            log.atError().log("Bean type must be provided before build()");
            throw new DslException("Bean type must be provided");
        }

        Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
        if (this.qualifier != null) {
            qualifiers.add(this.qualifier);
            log.atDebug().log("Added qualifier to build: {}", this.qualifier.getSimpleName());
        }

        IBeanSupplier<Bean> supplier = new BeanSupplier<>(Optional.ofNullable(this.provider),
                BeanDefinition.example(this.type, Optional.ofNullable(this.strategy),
                        Optional.ofNullable(this.name), qualifiers));
        log.atInfo().log("BeanSupplier built successfully for type: {}, provider: {}, name: {}",
                this.type.getSimpleName(), this.provider, this.name);
        log.atTrace().log("Exiting build() method");
        return supplier;
    }

    @Override
    public IBeanSupplierBuilder<Bean> name(String name) {
        log.atTrace().log("Entering name() method with name: {}", name);
        this.name = Objects.requireNonNull(name, "Bean name cannot be null");
        log.atDebug().log("Name set to: {}", this.name);
        log.atTrace().log("Exiting name() method");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> provider(String provider) {
        log.atTrace().log("Entering provider() method with provider: {}", provider);
        this.provider = Objects.requireNonNull(provider, "Bean provider cannot be null");
        log.atDebug().log("Provider set to: {}", this.provider);
        log.atTrace().log("Exiting provider() method");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> strategy(BeanStrategy strategy) {
        log.atTrace().log("Entering strategy() method with strategy: {}", strategy);
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        log.atDebug().log("Strategy set to: {}", this.strategy);
        log.atTrace().log("Exiting strategy() method");
        return this;
    }

    @Override
    public IBeanSupplierBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) {
        log.atTrace().log("Entering qualifier() method with qualifier: {}", qualifier);
        this.qualifier = Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        log.atDebug().log("Qualifier set to: {}", this.qualifier.getSimpleName());
        log.atTrace().log("Exiting qualifier() method");
        return this;
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Entering getDependencies() method");
        log.atTrace().log("Exiting getDependencies() method with empty set");
        return Set.of();
    }

    @Override
    public boolean isContextual() {
        log.atTrace().log("Entering isContextual() method");
        log.atTrace().log("Exiting isContextual() method with result: false");
        return false;
    }
}