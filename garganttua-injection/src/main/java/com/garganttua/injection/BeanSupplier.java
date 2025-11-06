package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanSupplier;
import com.garganttua.core.supplying.SupplyException;

public class BeanSupplier<Bean> implements IBeanSupplier<Bean> {

    private Optional<String> provider;
    private BeanDefinition<Bean> example;

    public BeanSupplier(Optional<String> provider, BeanDefinition<Bean> example) {
        this.example = Objects.requireNonNull(example, "Example cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
    }

    @Override
    public Optional<Bean> supply() throws SupplyException {
        if (DiContext.context == null) {
            throw new SupplyException("Context not built");
        }
        try {
            if (this.provider.isPresent())
                return DiContext.context.queryBean(provider.get(), example);
            return DiContext.context.queryBean(example);
        } catch (DiException e) {
            throw new SupplyException(e);
        }
    }

    @Override
    public Class<Bean> getSuppliedType() {
        return this.example.type();
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return Set.of();
    }
}
