package com.garganttua.injection.beans;

import java.util.Objects;
import java.util.Optional;

import com.garganttua.injection.DiContext;
import com.garganttua.injection.DiException;

public class BeanQuery<Bean> implements IBeanQuery<Bean> {

    private String provider = null;
    private BeanDefinition<Bean> definition;

    public BeanQuery(Optional<String> provider, BeanDefinition<Bean> definition) {
        Objects.requireNonNull(provider, "Strategy cannot be null");
        this.definition = Objects.requireNonNull(definition, "Definition cannot be null");

        provider.ifPresent(name -> this.provider = name);

    }

    public static IBeanQueryBuilder<?> builder() {
        return new BeanQueryBuilder<>();
    }

    @Override
    public Optional<Bean> execute() throws DiException {
        return DiContext.context.queryBean(Optional.ofNullable(this.provider), this.definition);
    }

}
