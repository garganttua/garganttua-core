package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.dsl.AbstractAutomaticLinkedBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.injection.IElementResolver;
import com.garganttua.core.injection.IInjectableElementResolver;
import com.garganttua.core.injection.IInjectableElementResolverBuilder;
import com.garganttua.core.injection.annotations.Resolver;
import com.garganttua.core.injection.context.resolver.InjectableElementResolver;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InjectableElementResolverBuilder
        extends
        AbstractAutomaticLinkedBuilder<IInjectableElementResolverBuilder, IInjectionContextBuilder, IInjectableElementResolver>
        implements IInjectableElementResolverBuilder {

    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    private final Map<IClass<? extends Annotation>, IElementResolver> manualResolvers = new HashMap<>();
    private final Map<IClass<? extends Annotation>, IElementResolver> autoDetectedResolvers = new HashMap<>();
    private final MultiSourceCollector<IClass<? extends Annotation>, IElementResolver> collector;

    private Set<IBuilderObserver<IInjectableElementResolverBuilder, IInjectableElementResolver>> observers = new HashSet<>();
    private final Set<String> packages = new HashSet<>();
    @Setter
    private IReflection reflection;

    @SuppressWarnings("unchecked")
    private static <K, V> ISupplier<Map<K, V>> mapSupplier(Map<K, V> map) {
        return new ISupplier<>() {
            @Override
            public Optional<Map<K, V>> supply() throws SupplyException {
                return Optional.of(map);
            }

            @Override
            public Type getSuppliedType() {
                return Map.class;
            }

            @Override
            public IClass<Map<K, V>> getSuppliedClass() {
                return (IClass<Map<K, V>>) (IClass<?>) IClass.getClass(Map.class);
            }
        };
    }

    public InjectableElementResolverBuilder(IInjectionContextBuilder link) {
        super(link);
        log.atTrace().log("Entering InjectableElementResolverBuilder constructor with link={}", link);

        this.collector = new MultiSourceCollector<>();
        collector.source(mapSupplier(manualResolvers), 0, SOURCE_MANUAL);
        collector.source(mapSupplier(autoDetectedResolvers), 1, SOURCE_AUTO_DETECTED);

        log.atTrace().log("Exiting InjectableElementResolverBuilder constructor");
    }

    @Override
    public IInjectableElementResolverBuilder withResolver(IClass<? extends Annotation> annotation,
            IElementResolver resolver) {
        log.atTrace().log("Entering withResolver(annotation={}, resolver={})", annotation, resolver);
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Objects.requireNonNull(resolver, "Resolver cannot be null");

        manualResolvers.put(annotation, resolver);
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
        Map<IClass<? extends Annotation>, IElementResolver> mergedResolvers = this.collector.build();
        InjectableElementResolver b = new InjectableElementResolver(mergedResolvers);
        this.notifyObserver(b);
        return b;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doAutoDetection() throws DslException {
        if (this.reflection == null) {
            log.atWarn().log("IReflection not set, skipping auto-detection");
            return;
        }

        IClass<Resolver> resolverIClass = reflection.getClass(Resolver.class);
        IClass<? extends Annotation> resolverAnnotation = (IClass<? extends Annotation>) resolverIClass;
        IClass<?> elementResolverInterface = reflection.getClass(IElementResolver.class);

        this.packages.stream()
                .flatMap(pkg -> reflection.getClassesWithAnnotation(pkg, resolverAnnotation).stream())
                .forEach(resolverClass -> {
                    try {
                        Resolver annotation = resolverClass.getAnnotation(resolverIClass);
                        if (annotation != null && elementResolverInterface.isAssignableFrom(resolverClass)) {
                            IElementResolver resolverInstance = (IElementResolver) reflection.newInstance(resolverClass);

                            for (Class<? extends Annotation> annotationType : annotation.annotations()) {
                                this.autoDetectedResolvers.put((IClass<? extends Annotation>) IClass.getClass(annotationType), resolverInstance);
                                log.atDebug().log("Auto-registered resolver {} for annotation {}",
                                        resolverClass.getName(), annotationType.getSimpleName());
                            }
                        } else {
                            log.atWarn().log(
                                    "Class {} annotated with @Resolver but does not implement IElementResolver",
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
