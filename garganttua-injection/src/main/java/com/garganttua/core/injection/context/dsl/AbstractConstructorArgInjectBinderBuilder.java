package com.garganttua.core.injection.context.dsl;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.dependency.DependentBuilderSupport;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractConstructorArgInjectBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends AbstractConstructorBinderBuilder<Constructed, Builder, Link>
        implements IDependentBuilder<Builder, IConstructorBinder<Constructed>> {

    private DependentBuilderSupport support;

    protected AbstractConstructorArgInjectBinderBuilder(Link link,
            Class<Constructed> construcetd) {
        super(link, construcetd);
        log.atTrace().log(
                "Entering AbstractConstructorArgInjectBinderBuilder constructor with link: {}, constructed class: {}",
                link, construcetd);
        this.support = new DependentBuilderSupport(Set.of(new DependencySpec(IInjectableElementResolverBuilder.class, DependencyPhase.AUTO_DETECT, true)));
        log.atInfo().log("AbstractConstructorArgInjectBinderBuilder initialized");
        log.atTrace().log("Exiting constructor");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.atTrace().log("Entering doAutoDetection");
        this.support.processAutoDetectionWithDependencies(this::doAutoDetectionWithDependency);
        log.atTrace().log("Exiting doAutoDetection");
    }

    private void doAutoDetectionWithDependency(Object dependency){
        if( dependency instanceof IInjectableElementResolver resolver )
            this.doAutoDetectionWithResolver(resolver);
    }

    private void doAutoDetectionWithResolver(IInjectableElementResolver resolver) {
        AtomicInteger counter = new AtomicInteger();
        Set<Resolved> resolved = resolver.resolve(this.findConstructor());
        log.atDebug().log("Resolved elements found: {}", resolved);

        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse(
                    (b, n) -> {
                        log.atInfo().log("Resolved constructor parameter {} with builder: {}", counter.get(), b);
                        this.withParam(counter.getAndIncrement(), b, n);
                    },
                    n -> {
                        log.atWarn().log(
                                "Constructor parameter {} not resolved, using NullSupplierBuilder for type: {}",
                                counter.get(), r.elementType());
                        this.withParam(counter.getAndIncrement(), new NullSupplierBuilder<>(r.elementType()), n);
                    });
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder provide(IObservableBuilder<?, ?> dependency) {
        this.support.provide(dependency);
        return (Builder) this;
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> use() {
        return this.support.use();
    }

    @Override
    public Set<Class<? extends IObservableBuilder<?, ?>>> require() {
        return this.support.require();
    }
}