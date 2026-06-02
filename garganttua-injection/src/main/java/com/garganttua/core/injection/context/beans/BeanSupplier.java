package com.garganttua.core.injection.context.beans;

import java.util.Optional;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.context.InjectionContext;
import com.garganttua.core.supply.SupplyException;

/**
 * {@link ContextualBeanSupplier} bound to the master {@link InjectionContext}, so
 * {@link #supply()} resolves the bean against the process-wide context.
 *
 * @param <Bean> the supplied bean type
 */
public class BeanSupplier<Bean> extends ContextualBeanSupplier<Bean> {

    /**
     * Builds a supplier for the given reference, optionally scoped to a named provider.
     *
     * @param provider the provider name to restrict the lookup to, or empty for all providers
     */
    public BeanSupplier(Optional<String> provider, BeanReference<Bean> query) {
        super(provider, query);
    }

    @Override
    public Optional<Bean> supply() throws SupplyException {
        return supply(InjectionContext.context);
    }


}
