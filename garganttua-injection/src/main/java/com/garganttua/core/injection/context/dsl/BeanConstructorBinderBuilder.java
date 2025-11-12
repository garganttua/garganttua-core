package com.garganttua.core.injection.context.dsl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.supplying.IObjectSupplier;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

import jakarta.annotation.Nullable;
import lombok.NonNull;

public class BeanConstructorBinderBuilder<Bean> extends
        AbstractConstructorBinderBuilder<Bean, IBeanConstructorBinderBuilder<Bean>, IBeanFactoryBuilder<Bean>>
        implements IBeanConstructorBinderBuilder<Bean> {

    private IInjectableElementResolver resolver;

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            IInjectableElementResolver resolver) {
        super(link, beanType);
        this.resolver = Objects.requireNonNull(resolver,
                "Injectable builder Registry cannot be null");
    }

    protected BeanConstructorBinderBuilder(BeanFactoryBuilder<Bean> link, Class<Bean> beanType,
            Optional<IInjectableElementResolver> resolver) {
        super(link, beanType);
        Objects.requireNonNull(resolver,
                "Injectable builder Registry cannot be null");
        this.resolver = resolver.orElse(null);
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

        if (this.resolver == null) {
            throw new DslException("Cannot do auto detection without registry");
        }

        Constructor<Bean> ctor = this.findMatchingConstructor();

        Parameter[] params = ctor.getParameters();

        for (int i = 0; i < params.length; i++) {
            Class<?> elementType = params[i].getType();
            Optional<IObjectSupplierBuilder<?, IObjectSupplier<?>>> builder;
            try {
                builder = this.resolver
                        .resolve(elementType, params[i]);
                boolean nullable = BeanPostConstructMethodBinderBuilder.isNullable(params[i]);
                if (builder.isPresent())
                    this.withParam(i, builder.get(), nullable);
                else
                    this.withParam(i, new NullObjectSupplierBuilder<>(elementType), nullable);
            } catch (DiException e) {
                throw new DslException(e);
            }
        }
    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(Arrays.asList(this.getParameterTypes()));
    }
}