package com.garganttua.core.injection.context.dsl;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supplying.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

import lombok.Setter;

public abstract class AbstractMethodArgInjectBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, Builder, Link, IMethodBinder<ExecutionReturn>>, Link>
        extends AbstractMethodBinderBuilder<ExecutionReturn, Builder, Link> {

    @Setter
    private IInjectableElementResolver resolver;

    protected AbstractMethodArgInjectBinderBuilder(IInjectableElementResolver resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        super(up, supplier);
        this.resolver = Objects.requireNonNull(resolver, "Resolver cannot be null");
    }

    protected AbstractMethodArgInjectBinderBuilder(IInjectableElementResolver resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier, boolean collection) throws DslException {
        super(up, supplier, collection);
        this.resolver = Objects.requireNonNull(resolver, "Resolver cannot be null");
    }

    protected AbstractMethodArgInjectBinderBuilder(Optional<IInjectableElementResolver> resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        super(up, supplier);
        Objects.requireNonNull(resolver, "Resolver cannot be null");
        this.resolver = resolver.orElse(null);
    }

    protected AbstractMethodArgInjectBinderBuilder(Optional<IInjectableElementResolver> resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier, boolean collection) throws DslException {
        super(up, supplier, collection);
        Objects.requireNonNull(resolver, "Resolver cannot be null");
        this.resolver = resolver.orElse(null);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.resolver == null) {
            throw new DslException("Cannot do auto detection without resolver");
        }

        Set<Resolved> resolved = this.resolver.resolve(this.findMethod());

        AtomicInteger counter = new AtomicInteger();
        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse((b, n) -> this.withParam(counter.getAndIncrement(), b, n),
                    n -> this.withParam(counter.getAndIncrement(), new NullObjectSupplierBuilder<>(r.elementType()),
                            n));
        });
    }
}
