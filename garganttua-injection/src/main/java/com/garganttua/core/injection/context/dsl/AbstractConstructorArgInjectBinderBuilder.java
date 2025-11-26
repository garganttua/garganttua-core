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
import com.garganttua.core.supply.dsl.NullObjectSupplierBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConstructorArgInjectBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends AbstractConstructorBinderBuilder<Constructed, Builder, Link> {

    @Setter
    private IInjectableElementResolver resolver;

    protected AbstractConstructorArgInjectBinderBuilder(IInjectableElementResolver resolver, Link link,
            Class<Constructed> construcetd) {
        super(link, construcetd);
        log.atTrace().log(
                "Entering AbstractConstructorArgInjectBinderBuilder constructor with resolver: {}, link: {}, constructed class: {}",
                resolver, link, construcetd);
        this.resolver = Objects.requireNonNull(resolver, "Injectable builder Registry cannot be null");
        log.atInfo().log("AbstractConstructorArgInjectBinderBuilder initialized with resolver: {}", resolver);
        log.atTrace().log("Exiting constructor");
    }

    protected AbstractConstructorArgInjectBinderBuilder(Optional<IInjectableElementResolver> resolver, Link link,
            Class<Constructed> construcetd) {
        super(link, construcetd);
        log.atTrace().log(
                "Entering AbstractConstructorArgInjectBinderBuilder constructor with optional resolver: {}, link: {}, constructed class: {}",
                resolver, link, construcetd);
        Objects.requireNonNull(resolver, "Injectable builder Registry cannot be null");
        this.resolver = resolver.orElse(null);
        log.atInfo().log("AbstractConstructorArgInjectBinderBuilder initialized with resolver: {}", this.resolver);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection");
        if (this.resolver == null) {
            log.atError().log("Cannot do auto detection without resolver");
            throw new DslException("Cannot do auto detection without resolver");
        }

        AtomicInteger counter = new AtomicInteger();
        Set<Resolved> resolved = this.resolver.resolve(this.findConstructor());
        log.atDebug().log("Resolved elements found: {}", resolved);

        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse(
                    (b, n) -> {
                        log.atInfo().log("Resolved constructor parameter {} with builder: {}", counter.get(), b);
                        this.withParam(counter.getAndIncrement(), b, n);
                    },
                    n -> {
                        log.atWarn().log(
                                "Constructor parameter {} not resolved, using NullObjectSupplierBuilder for type: {}",
                                counter.get(), r.elementType());
                        this.withParam(counter.getAndIncrement(), new NullObjectSupplierBuilder<>(r.elementType()), n);
                    });
        });

        log.atTrace().log("Exiting doAutoDetection");
    }
}