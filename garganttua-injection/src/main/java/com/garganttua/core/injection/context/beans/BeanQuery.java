package com.garganttua.core.injection.context.beans;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.injection.context.dsl.BeanQueryBuilder;

/**
 * {@link IBeanQuery} that resolves a {@link BeanReference} against the master
 * {@link InjectionContext}, optionally restricted to a named provider.
 *
 * @param <Bean> the queried bean type
 */
public class BeanQuery<Bean> implements IBeanQuery<Bean> {
    private static final Logger log = Logger.getLogger(BeanQuery.class);

    private String provider = null;
    private BeanReference<Bean> query;

    /**
     * Builds a query for the given reference, optionally scoped to a named provider.
     *
     * @param provider the provider name to restrict the lookup to, or empty for all providers
     */
    public BeanQuery(Optional<String> provider, BeanReference<Bean> query) {
        log.trace("Entering BeanQuery constructor with provider: {} and query: {}", provider, query);

        Objects.requireNonNull(provider, "Strategy cannot be null");
        Objects.requireNonNull(query, "Query cannot be null");

        provider.ifPresent(name -> {
            this.provider = name;
            log.debug("Provider set to: {}", this.provider);
        });

        this.query = query;

        log.debug("BeanQuery initialized with definition: {} and query: {}", query, this.provider);
        log.trace("Exiting BeanQuery constructor");
    }

    /**
     * Creates a fluent builder for assembling a {@link BeanQuery}.
     *
     * @return a new query builder
     */
    public static IBeanQueryBuilder<?> builder() {
        log.trace("Creating BeanQueryBuilder");
        IBeanQueryBuilder<?> builder = new BeanQueryBuilder<>();
        log.debug("BeanQueryBuilder created: {}", builder);
        return builder;
    }

    @Override
    public Optional<Bean> execute() throws DiException {
        log.trace("Executing BeanQuery with provider: {} and query: {}", provider, query);

        if (InjectionContext.context == null) {
            log.error("InjectionContext.context is null, cannot execute BeanQuery");
            throw new DiException("Context not built");
        }

        Optional<Bean> result = InjectionContext.context.queryBean(Optional.ofNullable(this.provider), this.query);
        log.debug("BeanQuery executed, result: {}", result.orElse(null));
        log.trace("Exiting execute with result: {}", result);

        return result;
    }
}
