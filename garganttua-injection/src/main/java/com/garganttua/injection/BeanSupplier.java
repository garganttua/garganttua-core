package com.garganttua.injection;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.injection.beans.BeanDefinition;

public class BeanSupplier<Bean> implements IBeanSupplier<Bean> {

    private Optional<String> provider;
    private BeanDefinition<Bean> example;

    public BeanSupplier(Optional<String> provider, BeanDefinition<Bean> example) {
        this.example = Objects.requireNonNull(example, "Example cannot be null");
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
    }

    @Override
    public Optional<Bean> getObject() throws DiException {
        if (this.provider.isPresent())
            return DiContext.context.queryBean(provider.get(), example);
        return DiContext.context.queryBean(example);
    }

    @Override
    public Class<Bean> getObjectClass() {
        return this.example.type();
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return Set.of();
    }
}
