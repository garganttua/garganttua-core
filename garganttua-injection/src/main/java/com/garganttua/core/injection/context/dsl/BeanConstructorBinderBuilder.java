package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

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
        AtomicInteger counter = new AtomicInteger();
        Set<Resolved> resolved = this.resolver.resolve(this.findMatchingConstructor());
        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse((b,n)->this.withParam(counter.getAndIncrement(), b, n),
                    n -> this.withParam(counter.getAndIncrement(), new NullObjectSupplierBuilder<>(r.elementType()), n));
        });

    }

    @Override
    public Set<Class<?>> getDependencies() {
        return new HashSet<>(Arrays.asList(this.getParameterTypes()));
    }
}