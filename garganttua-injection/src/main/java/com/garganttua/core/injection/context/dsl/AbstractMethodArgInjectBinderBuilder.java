package com.garganttua.core.injection.context.dsl;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.binders.IMethodBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractMethodBinderBuilder;
import com.garganttua.core.reflection.binders.dsl.IMethodBinderBuilder;
import com.garganttua.core.supply.dsl.ISupplierBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public abstract class AbstractMethodArgInjectBinderBuilder<ExecutionReturn, Builder extends IMethodBinderBuilder<ExecutionReturn, Builder, Link, Built>, Link, Built extends IMethodBinder<ExecutionReturn>>
        extends AbstractMethodBinderBuilder<ExecutionReturn, Builder, Link, Built> {
    private static final IDiagnostic log = Diagnostics.of(AbstractMethodArgInjectBinderBuilder.class);

    private static final Set<DependencySpec> INJECT_DEPS = Set.of(
            new DependencySpecBuilder(IClass.getClass(IInjectableElementResolverBuilder.class)).requireForAutoDetect().build());

    protected AbstractMethodArgInjectBinderBuilder(Link up,
            ISupplierBuilder<?, ?> supplier) throws DslException {
        this(up, supplier, false);
    }

    protected AbstractMethodArgInjectBinderBuilder(Link up,
            ISupplierBuilder<?, ?> supplier, boolean collection) throws DslException {
        super(up, supplier, collection, INJECT_DEPS);
        log.trace("Entering constructor with link: {}, supplier: {}, collection: {}",
                up, supplier, collection);
        log.trace("Exiting constructor");
    }

    @Override
    protected void doAutoDetection() throws DslException {
        log.trace("Entering doAutoDetection");
        // Auto-detection is handled via doAutoDetectionWithDependency
        log.trace("Exiting doAutoDetection");
    }

    @Override
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        if (dependency instanceof IInjectableElementResolver resolver) {
            this.doAutoDetectionWithResolver(resolver);
        }
    }

    @Override
    protected void doPreBuildWithDependency_(Object dependency) {
        // No additional pre-build handling needed
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No post-build handling needed
    }

    private void doAutoDetectionWithResolver(IInjectableElementResolver resolver) {
        AtomicInteger counter = new AtomicInteger();
        Set<Resolved> resolved = resolver.resolve(this.method().getParameters());
        log.debug("Resolved elements found: {}", resolved);

        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse(
                    (b, n) -> {
                        log.debug("Resolved method parameter {} with builder: {}", counter.get(), b);
                        this.withParam(counter.getAndIncrement(), b, n);
                    },
                    n -> {
                        log.warn(
                                "Method parameter {} not resolved, using NullSupplierBuilder for type: {}",
                                counter.get(), r.elementType());
                        this.withParam(counter.getAndIncrement(), new NullSupplierBuilder<>(r.elementType()), n);
                    });
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder provide(IObservableBuilder<?, ?> dependency) {
        return super.provide(dependency);
    }
}
