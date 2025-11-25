package com.garganttua.core.injection.context.beans;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.supplying.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanSupplier<Bean> implements IBeanSupplier<Bean> {

    private Optional<String> provider;
    private BeanDefinition<Bean> example;

    public BeanSupplier(Optional<String> provider, BeanDefinition<Bean> example) {
        log.atTrace().log("Entering BeanSupplier constructor with provider: {} and example: {}", provider, example);
        this.example = Objects.requireNonNull(example, "Example cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.atDebug().log("BeanSupplier initialized with example: {} and provider: {}", example, provider);
        log.atTrace().log("Exiting BeanSupplier constructor");
    }

    @Override
    public Optional<Bean> supply() throws SupplyException {
        log.atTrace().log("Entering supply for BeanSupplier with example: {} and provider: {}", example, provider);

        if (DiContext.context == null) {
            log.atError().log("DiContext.context is null, cannot supply bean");
            throw new SupplyException("Context not built");
        }

        try {
            Optional<Bean> result;
            if (this.provider.isPresent()) {
                log.atDebug().log("Querying bean with provider: {}", provider.get());
                result = DiContext.context.queryBean(provider.get(), example);
            } else {
                log.atDebug().log("Querying bean without provider");
                result = DiContext.context.queryBean(example);
            }

            log.atInfo().log("Bean supplied: {}", result.orElse(null));
            log.atTrace().log("Exiting supply with result: {}", result);
            return result;

        } catch (DiException e) {
            log.atError().log("Failed to supply bean for example {}: {}", example, e.getMessage());
            throw new SupplyException(e);
        }
    }

    @Override
    public Class<Bean> getSuppliedType() {
        log.atTrace().log("Returning supplied type for example {}: {}", example, example.type());
        return this.example.type();
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Returning empty dependencies set for example: {}", example);
        return Set.of();
    }
}
