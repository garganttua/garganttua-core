package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.injection.context.resolver.InjectableElementResolver;
import com.garganttua.core.reflection.utils.ObjectReflectionHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InjectableElementResolverBuilder
        extends
        AbstractAutomaticLinkedBuilder<IInjectableElementResolverBuilder, IInjectionContextBuilder, IInjectableElementResolver>
        implements IInjectableElementResolverBuilder {

    private final Map<Class<? extends Annotation>, IElementResolver> resolvers = new HashMap<>();
    private Set<IBuilderObserver<IInjectableElementResolverBuilder, IInjectableElementResolver>> observers = new HashSet<>();
    private final Set<String> packages = new HashSet<>();

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
        log.atDebug().log("Added resolver for annotation: {}", annotation);

        if (this.built != null) {
            this.built.addResolver(annotation, resolver);
            log.atDebug().log("Added resolver to already built InjectableElementResolver for annotation: {}",
                    annotation);
        }

        log.atTrace().log("Exiting withResolver");
        return this;
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
            log.atDebug().log("Context already built, immediately notified observer: {}", observer);
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

    @Override
    protected IInjectableElementResolver doBuild() throws DslException {
        InjectableElementResolver b = new InjectableElementResolver(this.resolvers);
        this.notifyObserver(b);
        return b;
    }

    @Override
    protected void doAutoDetection() throws DslException {
        this.packages.stream()
                .flatMap(package_ -> ObjectReflectionHelper.getClassesWithAnnotation(package_, Resolver.class).stream())
                .forEach(resolverClass -> {
                    try {
                        Resolver annotation = resolverClass.getAnnotation(Resolver.class);
                        if (annotation != null && IElementResolver.class.isAssignableFrom(resolverClass)) {
                            IElementResolver resolverInstance = (IElementResolver) resolverClass
                                    .getDeclaredConstructor().newInstance();

                            for (Class<? extends Annotation> annotationType : annotation.annotations()) {
                                this.withResolver(annotationType, resolverInstance);
                                log.atDebug().log("Auto-registered resolver {} for annotation {}",
                                        resolverClass.getSimpleName(), annotationType.getSimpleName());
                            }
                        } else {
                            log.atWarn().log(
                                    "Class {} annotated with @Resolver but does not implement IInjectableElementResolver",
                                    resolverClass.getName());
                        }
                    } catch (Exception e) {
                        log.atError().log("Failed to instantiate resolver {}: {}", resolverClass.getName(),
                                e.getMessage(), e);
                        throw new DslException("Failed to auto-detect resolver: " + resolverClass.getName(), e);
                    }
                });
    }

    @Override
    public IInjectableElementResolverBuilder withPackage(String packageName) {
        log.atDebug().log("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    @Override
    public IInjectableElementResolverBuilder withPackages(String[] packageNames) {
        log.atDebug().log("Adding {} packages", packageNames.length);
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

}
