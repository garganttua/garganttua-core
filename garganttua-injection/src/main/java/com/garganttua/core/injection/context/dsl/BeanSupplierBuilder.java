package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanReference;
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

    public BeanSupplierBuilder(Optional<String> provider, BeanReference<Bean> query) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with Optional provider: {} and query: {}",
                provider, query);
        Objects.requireNonNull(query, "Query cannot be null");
        log.atDebug().log("Query provided: {}", query);

        if (provider != null && provider.isPresent()) {
            initFromQuery(query);
            this.provider = provider.get();
            log.atDebug().log("Provider set from Optional: {}", this.provider);
        } else {
            initFromQuery(query);
        }
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    public BeanSupplierBuilder(BeanReference<Bean> query) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with query: {}", query);
        Objects.requireNonNull(query, "query cannot be null");
        initFromQuery(query);
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    public BeanSupplierBuilder(String provider, BeanReference<Bean> query) {
        log.atTrace().log("Entering BeanSupplierBuilder constructor with provider: {} and query: {}", provider,
                query);
        Objects.requireNonNull(provider, "Provider cannot be null");
        Objects.requireNonNull(query, "query cannot be null");

        initFromQuery(query);
        this.provider = provider;
        log.atDebug().log("Provider set to: {}", this.provider);
        log.atTrace().log("Exiting BeanSupplierBuilder constructor");
    }

    private void initFromQuery(BeanReference<Bean> query) {
        log.atTrace().log("Initializing BeanSupplierBuilder from query: {}", query);
        this.type = query.type();
        this.name = query.name().orElse(null);
        this.strategy = query.strategy().orElse(BeanStrategy.singleton);
        if (!query.qualifiers().isEmpty()) {
            this.qualifier = query.qualifiers().iterator().next();
            log.atDebug().log("Qualifier set from query: {}", this.qualifier.getSimpleName());
        }
        log.atDebug().log("Initialization from query complete. Type: {}, Name: {}, Strategy: {}, Qualifier: {}",
                this.type.getSimpleName(), this.name, this.strategy, this.qualifier);
        log.atTrace().log("Exiting initFromQuery method");
    }

    @Override
    public Type getSuppliedType() {
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
                new BeanReference<>(this.type, Optional.ofNullable(this.strategy),
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