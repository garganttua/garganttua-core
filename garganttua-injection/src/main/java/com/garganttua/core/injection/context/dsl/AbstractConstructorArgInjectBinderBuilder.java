package com.garganttua.core.injection.context.dsl;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.garganttua.core.dsl.DslException;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder;
import com.garganttua.core.supplying.dsl.NullObjectSupplierBuilder;

import lombok.Setter;

public abstract class AbstractConstructorArgInjectBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends
        AbstractConstructorBinderBuilder<Constructed, Builder, Link> {

    @Setter
    private IInjectableElementResolver resolver;

    protected AbstractConstructorArgInjectBinderBuilder(IInjectableElementResolver resolver, Link link,
            Class<Constructed> construcetd) {
        super(link, construcetd);
        this.resolver = Objects.requireNonNull(resolver,
                "Injectable builder Registry cannot be null");
    }

    protected AbstractConstructorArgInjectBinderBuilder(Optional<IInjectableElementResolver> resolver, Link link,
            Class<Constructed> construcetd) {
        super(link, construcetd);
        Objects.requireNonNull(resolver,
                "Injectable builder Registry cannot be null");
        this.resolver = resolver.orElse(null);
    }

    @Override
    protected void doAutoDetection() throws DslException {
        if (this.resolver == null) {
            throw new DslException("Cannot do auto detection without resolver");
        }
        AtomicInteger counter = new AtomicInteger();
        Set<Resolved> resolved = this.resolver.resolve(this.findConstructor());
        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse((b, n) -> this.withParam(counter.getAndIncrement(), b, n),
                    n -> this.withParam(counter.getAndIncrement(), new NullObjectSupplierBuilder<>(r.elementType()),
                            n));
        });
    }

}
