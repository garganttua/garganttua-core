package com.garganttua.core.injection.context.dsl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.observability.Logger;
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
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Builder that assembles an {@link IInjectableElementResolver} from manually registered
 * per-annotation {@link IElementResolver}s plus those auto-detected from classes annotated
 * with {@code @Resolver} in the configured packages.
 */
@Reflected
public class InjectableElementResolverBuilder
        extends
        AbstractAutomaticLinkedBuilder<IInjectableElementResolverBuilder, IInjectionContextBuilder, IInjectableElementResolver>
        implements IInjectableElementResolverBuilder {
    private static final Logger log = Logger.getLogger(InjectableElementResolverBuilder.class);

    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    private final Map<IClass<? extends Annotation>, IElementResolver> manualResolvers = new HashMap<>();
    private final Map<IClass<? extends Annotation>, IElementResolver> autoDetectedResolvers = new HashMap<>();
    private final MultiSourceCollector<IClass<? extends Annotation>, IElementResolver> collector;

    private Set<IBuilderObserver<IInjectableElementResolverBuilder, IInjectableElementResolver>> observers = new HashSet<>();
    private final Set<String> packages = new HashSet<>();
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

    /**
     * Sets the resolved {@link IReflection} used to scan packages for {@code @Resolver} classes.
     *
     * @param reflection the reflection facade
     */
    public void setReflection(IReflection reflection) {
        this.reflection = reflection;
    }

    /**
     * Creates a resolver builder linked to its parent injection context builder.
     *
     * @param link the parent injection context builder
     */
    public InjectableElementResolverBuilder(IInjectionContextBuilder link) {
        super(link);
        log.trace("Entering InjectableElementResolverBuilder constructor with link={}", link);

        this.collector = new MultiSourceCollector<>();
        collector.source(mapSupplier(manualResolvers), 0, SOURCE_MANUAL);
        collector.source(mapSupplier(autoDetectedResolvers), 1, SOURCE_AUTO_DETECTED);

        log.trace("Exiting InjectableElementResolverBuilder constructor");
    }

    /**
     * Registers a resolver for the given annotation. If the resolver was already built, the
     * resolver is also added to the live built instance.
     *
     * @param annotation the annotation the resolver handles; must not be {@code null}
     * @param resolver   the element resolver; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IInjectableElementResolverBuilder withResolver(IClass<? extends Annotation> annotation,
            IElementResolver resolver) {
        log.trace("Entering withResolver(annotation={}, resolver={})", annotation, resolver);
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Objects.requireNonNull(resolver, "Resolver cannot be null");

        manualResolvers.put(annotation, resolver);
        log.debug("Added resolver for annotation: {}", annotation);

        if (this.built != null) {
            this.built.addResolver(annotation, resolver);
            log.debug("Added resolver to already built InjectableElementResolver for annotation: {}",
                    annotation);
        }

        log.trace("Exiting withResolver");
        return this;
    }

    /**
     * Registers a build observer. If the resolver was already built, the observer is notified
     * immediately.
     *
     * @param observer the build observer; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IInjectableElementResolverBuilder observer(
            IBuilderObserver<IInjectableElementResolverBuilder, IInjectableElementResolver> observer) {
        log.trace("Entering observer(observer={})", observer);
        Objects.requireNonNull(observer, "Observer cannot be null");

        this.observers.add(observer);
        log.debug("Added observer: {}", observer);

        // If context is already built, notify the observer immediately
        if (this.built != null) {
            observer.handle(this.built);
            log.debug("Context already built, immediately notified observer: {}", observer);
        }

        log.trace("Exiting observer");
        return this;
    }

    private void notifyObserver(IInjectableElementResolver built) {
        log.trace("Entering notifyObserver(built={})", built);
        this.observers.parallelStream().forEach(observer -> {
            observer.handle(built);
            log.debug("Notified observer: {}", observer);
        });
        log.trace("Exiting notifyObserver");
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
            log.warn("IReflection not set, skipping auto-detection");
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
                                log.debug("Auto-registered resolver {} for annotation {}",
                                        resolverClass.getName(), annotationType.getSimpleName());
                            }
                        } else {
                            log.warn(
                                    "Class {} annotated with @Resolver but does not implement IElementResolver",
                                    resolverClass.getName());
                        }
                    } catch (Exception e) {
                        log.error("Failed to instantiate resolver {}: {}", resolverClass.getName(),
                                e.getMessage(), e);
                        throw new DslException("Failed to auto-detect resolver: " + resolverClass.getName(), e);
                    }
                });
    }

    /**
     * Adds a package to scan for {@code @Resolver}-annotated classes during auto-detection.
     *
     * @param packageName the package to scan; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IInjectableElementResolverBuilder withPackage(String packageName) {
        log.debug("Adding package: {}", packageName);
        this.packages.add(Objects.requireNonNull(packageName, "Package name cannot be null"));
        return this;
    }

    /**
     * Adds several packages to scan for {@code @Resolver}-annotated classes during auto-detection.
     *
     * @param packageNames the packages to scan; must not be {@code null}
     * @return this builder for chaining
     */
    @Override
    public IInjectableElementResolverBuilder withPackages(String[] packageNames) {
        log.debug("Adding {} packages", packageNames.length);
        Objects.requireNonNull(packageNames, "Package names cannot be null");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    /** {@return the packages configured for {@code @Resolver} auto-detection} */
    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

}
