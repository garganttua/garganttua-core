package com.garganttua.core.injection.context.beans;

import java.util.Optional;

import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.injection.context.DiContext;
import com.garganttua.core.supply.SupplyException;

public class BeanSupplier<Bean> extends ContextualBeanSupplier<Bean> implements IBeanSupplier<Bean> {

    public BeanSupplier(Optional<String> provider, BeanReference<Bean> query) {
        super(provider, query);
    }

    @Override
    public Optional<Bean> supply() throws SupplyException {
        return supply(DiContext.context);
    }


}
