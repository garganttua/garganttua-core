package com.garganttua.core.injection.context.beans;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IContextualBeanSupplier;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.supply.SupplyException;

/**
 * {@link IContextualBeanSupplier} that resolves a {@link BeanReference} against an
 * {@link IInjectionContext} supplied at {@code supply} time, optionally scoped to a
 * named provider.
 *
 * @param <Bean> the supplied bean type
 */
public class ContextualBeanSupplier<Bean> implements IContextualBeanSupplier<Bean> {
    private static final Logger log = Logger.getLogger(ContextualBeanSupplier.class);

    private Optional<String> provider;
    private BeanReference<Bean> query;

    /**
     * Builds a contextual supplier for the given reference, optionally scoped to a named provider.
     *
     * @param provider the provider name to restrict the lookup to, or empty for all providers
     */
    public ContextualBeanSupplier(Optional<String> provider, BeanReference<Bean> query) {
        log.trace("Entering ContextualBeanSupplier constructor with provider: {} and query: {}", provider, query);
        this.query = Objects.requireNonNull(query, "query cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        log.debug("ContextualBeanSupplier initialized with query: {} and provider: {}", query, provider);
        log.trace("Exiting ContextualBeanSupplier constructor");
    }

    @Override
    public Type getSuppliedType() {
        log.trace("Returning supplied type for query {}: {}", query, query.type());
        return this.query.type().getType();
    }

    @Override
    public Set<IClass<?>> dependencies() {
        log.trace("Returning empty dependencies set for query: {}", query);
        return Set.of();
    }

    @Override
    public IClass<IInjectionContext> getOwnerContextType() {
        return IClass.getClass(IInjectionContext.class);
    }

    @Override
    public IClass<Bean> getSuppliedClass() {
        return this.query.type();
    }

    @Override
    public Optional<Bean> supply(IInjectionContext context, Object... otherContexts) throws SupplyException {
        log.trace("Entering supply for BeanSupplier with query: {} and provider: {}", query, provider);

        if (InjectionContext.context == null) {
            log.error("InjectionContext.context is null, cannot supply bean");
            throw new SupplyException("Context not built");
        }

        try {
            Optional<Bean> result;
            if (this.provider.isPresent()) {
                log.debug("Querying bean with provider: {}", provider.get());
                result = context.queryBean(provider.get(), query);
            } else {
                log.debug("Querying bean without provider");
                result = context.queryBean(query);
            }

            log.debug("Bean supplied: {}", result.orElse(null));
            log.trace("Exiting supply with result: {}", result);
            return result;

        } catch (DiException e) {
            log.error("Failed to supply bean for query {}: {}", query, e.getMessage());
            throw new SupplyException(e);
        }
    }
}
