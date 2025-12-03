package com.garganttua.core.injection.context.beans;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanQuery<Bean> implements IBeanQuery<Bean> {

    private String provider = null;
    private BeanReference<Bean> query;

    public BeanQuery(Optional<String> provider, BeanReference<Bean> query) {
        log.atTrace().log("Entering BeanQuery constructor with provider: {} and query: {}", provider, query);

        Objects.requireNonNull(provider, "Strategy cannot be null");
        Objects.requireNonNull(query, "Query cannot be null");

        provider.ifPresent(name -> {
            this.provider = name;
            log.atDebug().log("Provider set to: {}", this.provider);
        });

        this.query = query;

        log.atInfo().log("BeanQuery initialized with definition: {} and query: {}", query, this.provider);
        log.atTrace().log("Exiting BeanQuery constructor");
    }

    public static IBeanQueryBuilder<?> builder() {
        log.atTrace().log("Creating BeanQueryBuilder");
        IBeanQueryBuilder<?> builder = new BeanQueryBuilder<>();
        log.atInfo().log("BeanQueryBuilder created: {}", builder);
        return builder;
    }

    @Override
    public Optional<Bean> execute() throws DiException {
        log.atTrace().log("Executing BeanQuery with provider: {} and query: {}", provider, query);

        if (DiContext.context == null) {
            log.atError().log("DiContext.context is null, cannot execute BeanQuery");
            throw new DiException("Context not built");
        }

        Optional<Bean> result = DiContext.context.queryBean(Optional.ofNullable(this.provider), this.query);
        log.atInfo().log("BeanQuery executed, result: {}", result.orElse(null));
        log.atTrace().log("Exiting execute with result: {}", result);

        return result;
    }
}
