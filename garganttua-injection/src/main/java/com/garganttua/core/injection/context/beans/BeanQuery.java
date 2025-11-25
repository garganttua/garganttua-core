package com.garganttua.core.injection.context.beans;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanQuery<Bean> implements IBeanQuery<Bean> {

    private String provider = null;
    private BeanDefinition<Bean> definition;

    public BeanQuery(Optional<String> provider, BeanDefinition<Bean> definition) {
        log.atTrace().log("Entering BeanQuery constructor with provider: {} and definition: {}", provider, definition);

        Objects.requireNonNull(provider, "Strategy cannot be null");
        Objects.requireNonNull(definition, "Definition cannot be null");

        provider.ifPresent(name -> {
            this.provider = name;
            log.atDebug().log("Provider set to: {}", this.provider);
        });

        this.definition = definition;

        log.atInfo().log("BeanQuery initialized with definition: {} and provider: {}", definition, this.provider);
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
        log.atTrace().log("Executing BeanQuery with provider: {} and definition: {}", provider, definition);

        if (DiContext.context == null) {
            log.atError().log("DiContext.context is null, cannot execute BeanQuery");
            throw new DiException("Context not built");
        }

        Optional<Bean> result = DiContext.context.queryBean(Optional.ofNullable(this.provider), this.definition);
        log.atInfo().log("BeanQuery executed, result: {}", result.orElse(null));
        log.atTrace().log("Exiting execute with result: {}", result);

        return result;
    }
}
