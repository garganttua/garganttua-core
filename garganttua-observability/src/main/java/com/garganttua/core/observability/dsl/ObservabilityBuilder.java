package com.garganttua.core.observability.dsl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencyPhase;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.reflection.dsl.IReflectionBuilder;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.annotations.Reflected;
import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.SupplyException;

/**
 * Root entry point of the observability DSL. Use {@link #create()} to obtain
 * a fresh builder, register observers (optionally filtered) via
 * {@link #subscribe}, optionally enable {@link #autoDetect(boolean)} scoped by
 * {@link #withPackage(String)}, then call {@link #build()} to materialize an
 * {@link ObservabilityBinding}.
 *
 * <p>This builder is annotated with {@link Bootstrap @Bootstrap} and shipped
 * with an {@code IBootstrapBuilderFactory} SPI descriptor so
 * {@code Bootstrap.autoDetect(true)} discovers it without manual wiring.
 * Engine builders that declare an optional dependency on
 * {@link IObservabilityBuilder} then receive this instance automatically
 * via the Bootstrap dep-resolution machinery, and self-attach their built
 * engine to the resulting {@link ObservabilityBinding}.
 *
 * <p>Observer registrations are aggregated through a {@link MultiSourceCollector}
 * with two prioritized sources:
 * <ol>
 *   <li>{@code "manual"} (priority 0) — observers passed to
 *       {@link #subscribe(IObserver)} by user code. Highest priority.</li>
 *   <li>{@code "auto-detected"} (priority 1) — observers discovered through the
 *       {@link Observer @Observer} annotation scan during
 *       {@link #doAutoDetectionWithDependency(Object)}.</li>
 * </ol>
 * If the same observer class appears in both sources, the manual entry wins.
 *
 * <p>Declares an optional dependency on {@link IInjectionContextBuilder} —
 * when present, {@code @Observer} classes are resolved as beans from the
 * {@link IInjectionContext} (so they can carry their own DI requirements).
 * When the bean lookup misses, the class is instantiated via the global
 * {@link IReflection} provider as a fallback.
 *
 * @since 2.0.0-ALPHA02
 */
@Bootstrap
@Reflected
public final class ObservabilityBuilder
        extends AbstractAutomaticDependentBuilder<IObservabilityBuilder, ObservabilityBinding>
        implements IObservabilityBuilder {

    private static final IDiagnostic log = Diagnostics.of(ObservabilityBuilder.class);

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            // Reflection feeds the @Observer scan during the auto-detect phase.
            DependencySpec.use(IClass.getClass(IReflectionBuilder.class), DependencyPhase.AUTO_DETECT),
            // Injection context is declared so Bootstrap (1) orders us AFTER
            // the InjectionContextBuilder and (2) auto-wires it via provide().
            // We INTERCEPT the call in provide() below — see the comment
            // there for why super.provide() must NOT be called for this dep.
            DependencySpec.use(IClass.getClass(IInjectionContextBuilder.class), DependencyPhase.BUILD));

    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_AUTO_DETECTED = "auto-detected";

    /**
     * Manual entries use a monotonically increasing key — multiple
     * {@code subscribe(...)} calls (including lambdas / method references
     * that share a JVM-generated class) all survive as distinct bindings.
     */
    private final Map<String, ObserverBindingBuilder> manualBindings = new LinkedHashMap<>();
    private final AtomicLong manualSeq = new AtomicLong();

    /**
     * Auto-detected entries are class-keyed so a re-run of
     * {@link #doAutoDetectionWithDependency(Object)} is idempotent.
     */
    private final Map<String, ObserverBindingBuilder> autoDetectedBindings = new LinkedHashMap<>();

    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());
    private final Set<IBuilderObserver<IObservabilityBuilder, ObservabilityBinding>> buildObservers = new HashSet<>();
    /** Captured at {@link #provide} time; consulted in {@link #doBuild} only. */
    private IInjectionContextBuilder injectionContextBuilder;
    private volatile ObservabilityBinding built;

    private ObservabilityBuilder() {
        super(DEPENDENCIES);
    }

    /**
     * Create a fresh, empty {@code ObservabilityBuilder}.
     */
    public static IObservabilityBuilder create() {
        return new ObservabilityBuilder();
    }

    // -- IObservabilityBuilder ------------------------------------------------

    @Override
    public IObserverBindingBuilder subscribe(IObserver<ObservableEvent> observer) {
        Objects.requireNonNull(observer, "observer");
        ObserverBindingBuilder binding = new ObserverBindingBuilder(observer, this);
        String key = "m#" + this.manualSeq.getAndIncrement() + ":"
                + observer.getClass().getName();
        this.manualBindings.put(key, binding);
        return binding;
    }

    @Override
    public ObservabilityBinding getBinding() {
        return this.built;
    }

    /**
     * Overridden to intercept the {@link IInjectionContextBuilder} dependency
     * — we keep a reference to it, but we must <strong>not</strong> route it
     * through {@link AbstractAutomaticDependentBuilder#provide}, which would
     * eagerly trigger {@code injCtxBuilder.build()} during Phase 1 (via
     * {@code BuilderDependency.tryResolve}). That early build publishes a
     * not-yet-initialised {@code InjectionContext} into the framework, which
     * breaks consumers downstream — e.g. {@code RuntimesBuilder.setupInjectionContext}
     * calls {@code injCtxBuilder.childContextFactory(...)} which forwards
     * to {@code built.registerChildContextFactory(...)}, requiring
     * lifecycle init that hasn't run yet. We postpone our use of the context
     * until {@link #doBuild()} (Phase 3), by which time Bootstrap has
     * built AND started it.
     */
    @Override
    public IObservabilityBuilder provide(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependency)
            throws DslException {
        if (dependency instanceof IInjectionContextBuilder injCtxBuilder) {
            this.injectionContextBuilder = injCtxBuilder;
            return this;
        }
        return super.provide(dependency);
    }

    @Override
    public IObservabilityBuilder observer(
            IBuilderObserver<IObservabilityBuilder, ObservabilityBinding> observer) {
        Objects.requireNonNull(observer, "build observer");
        this.buildObservers.add(observer);
        if (this.built != null) {
            observer.handle(this.built);
        }
        return this;
    }

    // -- IPackageableBuilder --------------------------------------------------

    @Override
    public IObservabilityBuilder withPackage(String packageName) {
        Objects.requireNonNull(packageName, "package name");
        this.packages.add(packageName);
        return this;
    }

    @Override
    public IObservabilityBuilder withPackages(String[] packageNames) {
        Objects.requireNonNull(packageNames, "package names");
        for (String pkg : packageNames) {
            this.withPackage(pkg);
        }
        return this;
    }

    @Override
    public String[] getPackages() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected String[] getPackagesForScanning() {
        return this.packages.toArray(new String[0]);
    }

    @Override
    protected IReflection getReflection() {
        try {
            return IClass.getReflection();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    // -- AbstractAutomaticDependentBuilder hooks -----------------------------

    @Override
    protected void doAutoDetection() throws DslException {
        // Intentional no-op: @Observer scanning happens in
        // doAutoDetectionWithDependency(IInjectionContext) so observers can
        // be resolved as managed beans. Without an IInjectionContext on the
        // critical path, autoDetect simply does nothing here — users without
        // DI register observers explicitly via subscribe(...).
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        if (!(dependency instanceof IReflection reflection)) {
            return;
        }

        IClass<Observer> annotation = IClass.getClass(Observer.class);
        List<IClass<?>> candidates = new ArrayList<>();
        if (this.packages.isEmpty()) {
            candidates.addAll(reflection.getClassesWithAnnotation(annotation));
        } else {
            for (String pkg : this.packages) {
                candidates.addAll(reflection.getClassesWithAnnotation(pkg, annotation));
            }
        }

        for (IClass<?> klass : candidates) {
            Observer meta = klass.getAnnotation(annotation);
            if (meta == null) {
                continue;
            }
            Object instance;
            try {
                instance = reflection.newInstance(klass);
            } catch (ReflectionException e) {
                log.warn("@Observer class {} could not be instantiated (no-arg ctor required): {}",
                        klass.getName(), e.getMessage());
                continue;
            }
            if (!(instance instanceof IObserver<?>)) {
                log.warn("@Observer class {} does not implement IObserver — skipping",
                        klass.getName());
                continue;
            }
            IObserver<ObservableEvent> observer = (IObserver<ObservableEvent>) instance;
            ObserverBindingBuilder binding = new ObserverBindingBuilder(observer, this);
            if (meta.events().length > 0) {
                binding.onlyEvents(meta.events());
            }
            if (meta.sources().length > 0) {
                binding.matchingAnySource(meta.sources());
            }
            this.autoDetectedBindings.put(klass.getName(), binding);
            log.debug("@Observer auto-registered: {} (events={}, sources={})",
                    klass.getSimpleName(), meta.events().length, meta.sources().length);
        }
    }

    @Override
    protected ObservabilityBinding doBuild() throws DslException {
        Map<String, ObserverBindingBuilder> aggregated = computeBindings();
        log.debug("Building ObservabilityBinding from {} merged observer source(s)",
                aggregated.size());

        List<IObserver<ObservableEvent>> wrappers = new ArrayList<>(aggregated.size());
        for (ObserverBindingBuilder b : aggregated.values()) {
            wrappers.add(b.buildWrapper());
        }
        ObservabilityBinding binding = new ObservabilityBinding(wrappers);
        this.built = binding;

        // Publish the binding as a DI bean, if a context builder was provided.
        // At Phase 3 of Bootstrap the injection context has been built AND
        // its lifecycle is init/started, so this is safe.
        if (this.injectionContextBuilder != null) {
            try {
                registerBindingAsBean(this.injectionContextBuilder.build(), binding);
            } catch (DslException e) {
                log.warn("Could not resolve IInjectionContext for bean registration: {}",
                        e.getMessage());
            }
        }

        for (IBuilderObserver<IObservabilityBuilder, ObservabilityBinding> o : this.buildObservers) {
            try {
                o.handle(binding);
            } catch (RuntimeException e) {
                log.warn("Build observer threw: {}", e.getMessage());
            }
        }
        return binding;
    }

    /**
     * Aggregate the two observer sources through a {@link MultiSourceCollector}.
     * Manual subscriptions take precedence over auto-detected ones when the
     * same class shows up in both.
     */
    private Map<String, ObserverBindingBuilder> computeBindings() {
        MultiSourceCollector<String, ObserverBindingBuilder> collector = new MultiSourceCollector<>();
        collector.source(bindingSupplier(this.manualBindings), 0, SOURCE_MANUAL);
        collector.source(bindingSupplier(this.autoDetectedBindings), 1, SOURCE_AUTO_DETECTED);
        return collector.build();
    }

    private static ISupplier<Map<String, ObserverBindingBuilder>> bindingSupplier(
            Map<String, ObserverBindingBuilder> snapshot) {
        return new ISupplier<>() {
            @Override
            public Optional<Map<String, ObserverBindingBuilder>> supply() throws SupplyException {
                return Optional.of(snapshot);
            }

            @Override
            public Type getSuppliedType() {
                return Map.class;
            }

            @Override
            @SuppressWarnings({ "unchecked", "rawtypes" })
            public IClass<Map<String, ObserverBindingBuilder>> getSuppliedClass() {
                return (IClass) IClass.getClass(Map.class);
            }
        };
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // No-op
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No-op — bean registration is performed inline in doBuild() using
        // the IInjectionContextBuilder captured in provide().
    }

    /**
     * Publish the freshly-built {@link ObservabilityBinding} into the
     * injection context so user-defined beans can {@code @Inject} it.
     * Registered under the standard {@code "garganttua"} provider with the
     * canonical name {@code "ObservabilityBinding"}. Failure is logged but
     * never propagated — a broken DI registration must not abort the
     * outer Bootstrap build.
     */
    private static void registerBindingAsBean(IInjectionContext context, ObservabilityBinding binding) {
        BeanReference<ObservabilityBinding> ref = new BeanReference<>(
                IClass.getClass(ObservabilityBinding.class),
                Optional.of(BeanStrategy.singleton),
                Optional.of("ObservabilityBinding"),
                Set.of());
        try {
            context.addBean(Predefined.BeanProviders.garganttua.toString(), ref, binding);
            log.debug("ObservabilityBinding registered as singleton bean in InjectionContext");
        } catch (DiException e) {
            log.warn("Failed to register ObservabilityBinding as bean: {}", e.getMessage());
        }
    }
}
