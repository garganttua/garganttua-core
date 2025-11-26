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
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;
import com.garganttua.core.supply.dsl.NullObjectSupplierBuilder;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractMethodArgInjectBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, Builder, Link, Built>, Link, Built extends IMethodBinder<ExecutionReturn>>
        extends AbstractMethodBinderBuilder<ExecutionReturn, Builder, Link, Built> {

    @Setter
    private IInjectableElementResolver resolver;

    protected AbstractMethodArgInjectBinderBuilder(IInjectableElementResolver resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        super(up, supplier);
        log.atTrace().log("Entering constructor with resolver: {}, link: {}, supplier: {}", resolver, up, supplier);
        this.resolver = Objects.requireNonNull(resolver, "Resolver cannot be null");
        log.atInfo().log("AbstractMethodArgInjectBinderBuilder initialized with resolver: {}", resolver);
        log.atTrace().log("Exiting constructor");
    }

    protected AbstractMethodArgInjectBinderBuilder(IInjectableElementResolver resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier, boolean collection) throws DslException {
        super(up, supplier, collection);
        log.atTrace().log("Entering constructor with resolver: {}, link: {}, supplier: {}, collection: {}", resolver,
                up, supplier, collection);
        this.resolver = Objects.requireNonNull(resolver, "Resolver cannot be null");
        log.atInfo().log("AbstractMethodArgInjectBinderBuilder initialized with resolver: {}", resolver);
        log.atTrace().log("Exiting constructor");
    }

    protected AbstractMethodArgInjectBinderBuilder(Optional<IInjectableElementResolver> resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier) throws DslException {
        super(up, supplier);
        log.atTrace().log("Entering constructor with optional resolver: {}, link: {}, supplier: {}", resolver, up,
                supplier);
        Objects.requireNonNull(resolver, "Resolver cannot be null");
        this.resolver = resolver.orElse(null);
        log.atInfo().log("AbstractMethodArgInjectBinderBuilder initialized with resolver: {}", this.resolver);
        log.atTrace().log("Exiting constructor");
    }

    protected AbstractMethodArgInjectBinderBuilder(Optional<IInjectableElementResolver> resolver, Link up,
            IObjectSupplierBuilder<?, ?> supplier, boolean collection) throws DslException {
        super(up, supplier, collection);
        log.atTrace().log("Entering constructor with optional resolver: {}, link: {}, supplier: {}, collection: {}",
                resolver, up, supplier, collection);
        Objects.requireNonNull(resolver, "Resolver cannot be null");
        this.resolver = resolver.orElse(null);
        log.atInfo().log("AbstractMethodArgInjectBinderBuilder initialized with resolver: {}", this.resolver);
        log.atTrace().log("Exiting constructor");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection");
        if (this.resolver == null) {
            log.atError().log("Cannot do auto detection without resolver");
            throw new DslException("Cannot do auto detection without resolver");
        }

        Set<Resolved> resolved = this.resolver.resolve(this.findMethod());
        log.atDebug().log("Resolved method parameters: {}", resolved);

        AtomicInteger counter = new AtomicInteger();
        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse(
                    (b, n) -> {
                        log.atInfo().log("Resolved method parameter {} with builder: {}", counter.get(), b);
                        this.withParam(counter.getAndIncrement(), b, n);
                    },
                    n -> {
                        log.atWarn().log(
                                "Method parameter {} not resolved, using NullObjectSupplierBuilder for type: {}",
                                counter.get(), r.elementType());
                        this.withParam(counter.getAndIncrement(), new NullObjectSupplierBuilder<>(r.elementType()), n);
                    });
        });

        log.atTrace().log("Exiting doAutoDetection");
    }
}