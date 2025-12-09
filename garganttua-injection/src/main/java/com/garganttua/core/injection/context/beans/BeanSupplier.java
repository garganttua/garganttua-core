package com.garganttua.core.injection.context.beans;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.supply.SupplyException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanSupplier<Bean> implements IBeanSupplier<Bean> {

    private Optional<String> provider;
    private BeanReference<Bean> query;

    public BeanSupplier(Optional<String> provider, BeanReference<Bean> query) {
        log.atTrace().log("Entering BeanSupplier constructor with provider: {} and query: {}", provider, query);
        this.query = Objects.requireNonNull(query, "query cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.atDebug().log("BeanSupplier initialized with query: {} and provider: {}", query, provider);
        log.atTrace().log("Exiting BeanSupplier constructor");
    }

    @Override
    public Optional<Bean> supply() throws SupplyException {
        log.atTrace().log("Entering supply for BeanSupplier with query: {} and provider: {}", query, provider);

        if (DiContext.context == null) {
            log.atError().log("DiContext.context is null, cannot supply bean");
            throw new SupplyException("Context not built");
        }

        try {
            Optional<Bean> result;
            if (this.provider.isPresent()) {
                log.atDebug().log("Querying bean with provider: {}", provider.get());
                result = DiContext.context.queryBean(provider.get(), query);
            } else {
                log.atDebug().log("Querying bean without provider");
                result = DiContext.context.queryBean(query);
            }

            log.atInfo().log("Bean supplied: {}", result.orElse(null));
            log.atTrace().log("Exiting supply with result: {}", result);
            return result;

        } catch (DiException e) {
            log.atError().log("Failed to supply bean for query {}: {}", query, e.getMessage());
            throw new SupplyException(e);
        }
    }

    @Override
    public Type getSuppliedType() {
        log.atTrace().log("Returning supplied type for query {}: {}", query, query.type());
        return this.query.type();
    }

    @Override
    public Set<Class<?>> getDependencies() {
        log.atTrace().log("Returning empty dependencies set for query: {}", query);
        return Set.of();
    }
}
