package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.AbstractLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.resolver.InjectableElementResolver;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InjectableElementResolverBuilder
        extends AbstractLinkedBuilder<IInjectionContextBuilder, IInjectableElementResolver>
        implements IInjectableElementResolverBuilder {

    private final Map<Class<? extends Annotation>, IElementResolver> resolvers = new HashMap<>();
    private IInjectableElementResolver built;
    private Set<IBuilderObserver<IInjectableElementResolverBuilder, IInjectableElementResolver>> observers = new HashSet<>();

    public InjectableElementResolverBuilder(IInjectionContextBuilder link) {
        super(link);
        log.atTrace().log("Entering InjectableElementResolverBuilder constructor with link={}", link);
        log.atTrace().log("Exiting InjectableElementResolverBuilder constructor");
    }

    @Override
    public IInjectableElementResolverBuilder withResolver(Class<? extends Annotation> annotation,
            IElementResolver resolver) {
        log.atTrace().log("Entering withResolver(annotation={}, resolver={})", annotation, resolver);
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Objects.requireNonNull(resolver, "Resolver cannot be null");

        resolvers.put(annotation, resolver);
        log.atInfo().log("Added resolver for annotation: {}", annotation);

        if (this.built != null) {
            this.built.addResolver(annotation, resolver);
            log.atDebug().log("Added resolver to already built InjectableElementResolver for annotation: {}",
                    annotation);
        }

        log.atTrace().log("Exiting withResolver");
        return this;
    }

    @Override
    public IInjectableElementResolver build() throws DslException {
        log.atTrace().log("Entering build()");
        if (this.built == null) {
            this.built = new InjectableElementResolver(this.resolvers);
            this.notifyObserver(this.built);
            log.atInfo().log("Built new InjectableElementResolver with {} resolvers", this.resolvers.size());
        } else {
            log.atDebug().log("Returning existing built InjectableElementResolver");
        }
        log.atTrace().log("Exiting build()");
        return this.built;
    }

    @Override
    public IInjectableElementResolverBuilder observer(
            IBuilderObserver<IInjectableElementResolverBuilder, IInjectableElementResolver> observer) {
        log.atTrace().log("Entering observer(observer={})", observer);
        Objects.requireNonNull(observer, "Observer cannot be null");

        this.observers.add(observer);
        log.atDebug().log("Added observer: {}", observer);

        // If context is already built, notify the observer immediately
        if (this.built != null) {
            observer.handle(this.built);
            log.atInfo().log("Context already built, immediately notified observer: {}", observer);
        }

        log.atTrace().log("Exiting observer");
        return this;
    }

    private void notifyObserver(IInjectableElementResolver built) {
        log.atTrace().log("Entering notifyObserver(built={})", built);
        this.observers.parallelStream().forEach(observer -> {
            observer.handle(built);
            log.atDebug().log("Notified observer: {}", observer);
        });
        log.atTrace().log("Exiting notifyObserver");
    }

}
