package com.garganttua.core.injection.context.dsl;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.dependency.DependencySpecBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.Resolved;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IConstructor;
import com.garganttua.core.reflection.binders.IConstructorBinder;
import com.garganttua.core.reflection.binders.dsl.AbstractConstructorBinderBuilder;
import com.garganttua.core.reflection.binders.dsl.IConstructorBinderBuilder;
import com.garganttua.core.supply.dsl.NullSupplierBuilder;

public abstract class AbstractConstructorArgInjectBinderBuilder<Constructed, Builder extends IConstructorBinderBuilder<Constructed, Builder, Link, IConstructorBinder<Constructed>>, Link>
        extends AbstractConstructorBinderBuilder<Constructed, Builder, Link> {
    private static final Logger log = Logger.getLogger(AbstractConstructorArgInjectBinderBuilder.class);

    private final IClass<Constructed> constructedClass;

    protected AbstractConstructorArgInjectBinderBuilder(Link link,
            IClass<Constructed> constructed) {
        super(link, constructed, Set.of(new DependencySpecBuilder(IClass.getClass(IInjectableElementResolverBuilder.class)).requireForAutoDetect().build()));
        this.constructedClass = constructed;
        log.trace(
                "Entering AbstractConstructorArgInjectBinderBuilder constructor with link: {}, constructed class: {}",
                link, constructed);
        log.debug("AbstractConstructorArgInjectBinderBuilder initialized");
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
        // Find the @Inject constructor on the target class
        IClass<Inject> injectClass = IClass.getClass(Inject.class);
        IConstructor<?> targetConstructor = Arrays.stream(constructedClass.getDeclaredConstructors())
                .filter(c -> c.isAnnotationPresent(injectClass))
                .findFirst()
                .orElse(null);

        if (targetConstructor == null) {
            log.debug("No @Inject constructor found for {}, skipping parameter resolution",
                    constructedClass.getSimpleName());
            return;
        }

        AtomicInteger counter = new AtomicInteger();
        Set<Resolved> resolved = resolver.resolve(targetConstructor.getParameters());
        log.debug("Resolved elements found: {}", resolved);

        resolved.stream().forEach(r -> {
            r.ifResolvedOrElse(
                    (b, n) -> {
                        log.debug("Resolved constructor parameter {} with builder: {}", counter.get(), b);
                        this.withParam(counter.getAndIncrement(), b, n);
                    },
                    n -> {
                        log.warn(
                                "Constructor parameter {} not resolved, using NullSupplierBuilder for type: {}",
                                counter.get(), r.elementType());
                        this.withParam(counter.getAndIncrement(), new NullSupplierBuilder<>(r.elementType()), n);
                    });
        });
    }

    protected IClass<Constructed> getConstructedClass() {
        return this.constructedClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder provide(IObservableBuilder<?, ?> dependency) {
        return super.provide(dependency);
    }
}
