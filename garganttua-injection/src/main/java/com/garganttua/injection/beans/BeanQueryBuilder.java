package com.garganttua.injection.beans;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Qualifier;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.BeanDefinition;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IBeanQuery;
import com.garganttua.core.injection.IBeanQueryBuilder;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BeanQueryBuilder<Bean> implements IBeanQueryBuilder<Bean> {

    private Class<Bean> type;
    private String provider;
    private BeanStrategy strategy;
    private Class<? extends Annotation> qualifier;
    private String name;

    @Override
    public IBeanQuery<Bean> build() throws DslException {
        Set<Class<? extends Annotation>> qualifiers = new HashSet<>();
        if (this.qualifier != null) {
            qualifiers.add(this.qualifier);
        }
        return new BeanQuery<>(
                Optional.ofNullable(this.provider),
                BeanDefinition.example(
                        this.type,
                        Optional.ofNullable(this.strategy),
                        Optional.ofNullable(this.name),
                        qualifiers));
    }

    @Override
    public IBeanQueryBuilder<Bean> type(Class<Bean> type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> name(String name) {
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> qualifier(Class<? extends Annotation> qualifier) throws DiException {
        this.qualifier = Objects.requireNonNull(qualifier, "Qualifier cannot be null");
        if (qualifier.getAnnotation(Qualifier.class) == null) {
            throw new DiException("Qualifier must have @Qualifier annotation");
        }
        ;
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> strategy(BeanStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "Strategy cannot be null");
        return this;
    }

    @Override
    public IBeanQueryBuilder<Bean> provider(String provider) {
        this.provider = Objects.requireNonNull(provider, "Provider cannot be null");
        return this;
    }
}
