package com.garganttua.injection.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.dsl.DslException;
import com.garganttua.injection.IInjectableBuilderRegistry;
import com.garganttua.injection.spec.supplier.IObjectSupplier;
import com.garganttua.injection.spec.supplier.builder.supplier.IObjectSupplierBuilder;
import com.garganttua.injection.supplier.builder.binder.AbstractConstructorBinderBuilder;

public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    private IInjectableBuilderRegistry injectableBuilderRegistry;

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            IInjectableBuilderRegistry injectableBuilderRegistry) {
        super(link, beanType);
        this.injectableBuilderRegistry = Objects.requireNonNull(injectableBuilderRegistry,
                "Injectable builder Registry cannot be null");
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            Optional<IInjectableBuilderRegistry> injectableBuilderRegistry) {
        super(link, beanType);
        Objects.requireNonNull(injectableBuilderRegistry,
                "Injectable builder Registry cannot be null");
        this.injectableBuilderRegistry = injectableBuilderRegistry.orElse(null);
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType) {
        super(link, beanType);
    }

    @Override
    protected IBeanConstructorBinderBuilder<Bean> getBuilder() {
        return this;
    }

    @Override
    protected void doAutoDetection() throws DslException {

        if( this.injectableBuilderRegistry == null ){
            throw new DslException("Cannot do auto detection without registry");
        }

        Constructor<Bean> ctor = this.findMatchingConstructor();

        Parameter[] params = ctor.getParameters();

        for (int i = 0; i < params.length; i++) {
            Class<?> elementType = params[i].getType();
            Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder = this.injectableBuilderRegistry
                    .createBuilder(elementType, params[i]);
            if (builder.isPresent())
                this.withParam(i, builder.get(), true);
        }
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(Arrays.asList(this.getParameterTypes()));
    }
}