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
import com.garganttua.core.observability.Logger;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.MultiSourceCollector;
import com.garganttua.core.dsl.annotations.ConfigurableBuilder;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.BeanStrategy;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.Predefined;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.IClass;
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
 *   <li>{@code "auto-detected"} (priority 1) — observers discovered by
 *       querying the {@link IInjectionContext} for beans whose class carries
 *       the {@link Observer @Observer} qualifier annotation.</li>
 * </ol>
 *
 * <p>Sources ({@link IObservable}) are <strong>not</strong> auto-detected
 * from the injection context — the framework would otherwise try to
 * instantiate them as managed beans, but the IObservable instances we
 * actually care about are produced by the engine builders themselves
 * (Workflow, Runtime, Bootstrap, …). Those builders depend on this builder
 * and call {@link ObservabilityBinding#attachSource(IObservable)} on the
 * binding via {@link #getBinding()} after their own {@code doBuild()}.
 * Two extra paths let users plug in ad-hoc sources:
 * <ul>
 *   <li>{@link #observe(IObservable...)} — declared on the builder.</li>
 *   <li>{@link ObservabilityBinding#attachSource(IObservable)} — post-build.</li>
 * </ul>
 *
 * <p>Declares an optional dependency on {@link IInjectionContextBuilder} —
 * the context is used in two ways:
 * <ul>
 *   <li>During autodetect, to list beans annotated with {@code @Observer}.</li>
 *   <li>At the end of {@link #doBuild()}, to publish the freshly-built
 *       {@link ObservabilityBinding} as a singleton bean so other beans can
 *       {@code @Inject} it.</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA02
 */
@Bootstrap
@Reflected
@ConfigurableBuilder("observability")
public final class ObservabilityBuilder
        extends AbstractAutomaticDependentBuilder<IObservabilityBuilder, ObservabilityBinding>
        implements IObservabilityBuilder {

    private static final Logger log = Logger.getLogger(ObservabilityBuilder.class);

    private static final Set<DependencySpec> DEPENDENCIES = Set.of(
            // CONFIGURATION stage: receive the IInjectionContextBuilder BEFORE
            // its build, register @Observer as a qualifier so the bean
            // provider auto-discovers user-annotated observer classes.
            DependencySpec.configure(IClass.getClass(IInjectionContextBuilder.class)),
            // BUILD stage: receive the built IInjectionContext post-build to
            // (1) query @Observer beans during our autodetect, and (2)
            // publish the binding as a singleton bean for downstream
            // consumers.
            DependencySpec.use(IClass.getClass(IInjectionContextBuilder.class)));

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

    /**
     * Manually declared observable sources (via {@link #observe(IObservable...)}).
     * Counter-keyed so multiple declarations don't collapse — same rationale
     * as {@link #manualSeq} for observers.
     */
    private final Map<String, IObservable> manualObservables = new LinkedHashMap<>();
    private final AtomicLong manualObservableSeq = new AtomicLong();

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
    public IObservabilityBuilder observe(IObservable... sources) {
        Objects.requireNonNull(sources, "sources");
        for (IObservable src : sources) {
            Objects.requireNonNull(src, "source");
            String key = "m#" + this.manualObservableSeq.getAndIncrement() + ":"
                    + src.getClass().getName();
            this.manualObservables.put(key, src);
        }
        return this;
    }

    @Override
    public ObservabilityBinding getBinding() {
        return this.built;
    }

    /**
     * Capture the {@link IInjectionContextBuilder} ref as soon as Bootstrap
     * provides it (Phase 1). The actual configuration mutation
     * ({@code withQualifier(@Observer)}) happens in
     * {@link #doConfigureWithDependencyBuilder} during Bootstrap's
     * Phase 1.5 CONFIGURATION stage.
     */
    @Override
    public IObservabilityBuilder provide(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependency)
            throws DslException {
        if (dependency instanceof IInjectionContextBuilder injCtxBuilder) {
            this.injectionContextBuilder = injCtxBuilder;
        }
        return super.provide(dependency);
    }

    /**
     * CONFIGURATION-stage hook: register {@code @Observer} as a qualifier
     * on the {@link IInjectionContextBuilder} BEFORE its build. Bootstrap
     * fires this exactly once per Bootstrap lifetime, so the hook is
     * safely idempotent without manual guarding.
     */
    @Override
    protected void doConfigureWithDependencyBuilder(
            com.garganttua.core.dsl.IObservableBuilder<?, ?> dependencyBuilder)
            throws DslException {
        if (dependencyBuilder instanceof IInjectionContextBuilder injCtxBuilder) {
            injCtxBuilder.withQualifier(IClass.getClass(Observer.class));
            log.debug("Registered @Observer as a qualifier on the InjectionContextBuilder");
        }
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

    // -- AbstractAutomaticDependentBuilder hooks -----------------------------

    @Override
    protected void doAutoDetection() throws DslException {
        // Bridge: the standard framework hook for autodetect-with-dependency
        // never fires for us because our provide() override intercepts the
        // IInjectionContextBuilder before super.provide() can route it
        // through the dep machinery (which would eagerly build the context
        // and break lifecycle ordering — see provide()). Instead, we call
        // doAutoDetectionWithDependency manually here with the cached,
        // already-init'd context the framework has produced.
        if (this.injectionContextBuilder == null) {
            log.debug("autoDetect skipped: no IInjectionContextBuilder provided");
            return;
        }
        IInjectionContext context = this.injectionContextBuilder.build();
        doAutoDetectionWithDependency(context);
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void doAutoDetectionWithDependency(Object dependency) throws DslException {
        if (!(dependency instanceof IInjectionContext context)) {
            return;
        }
        // Query beans carrying the @Observer qualifier. Both @Observer and
        // @Observable are registered as qualifiers in provide(), so the bean
        // provider has already turned every @Observer-annotated class into a
        // managed bean.
        BeanReference observerQuery = new BeanReference(
                null, Optional.empty(), Optional.empty(),
                Set.of(IClass.getClass(Observer.class)));
        List<Object> beans;
        try {
            beans = context.queryBeans(observerQuery);
        } catch (DiException e) {
            log.warn("Failed to query @Observer beans from InjectionContext: {}", e.getMessage());
            return;
        }

        for (Object bean : beans) {
            if (!(bean instanceof IObserver<?>)) {
                log.warn("@Observer bean {} does not implement IObserver — skipping",
                        bean.getClass().getName());
                continue;
            }
            IClass<?> beanClass = IClass.getClass(bean.getClass());
            Observer meta = beanClass.getAnnotation(IClass.getClass(Observer.class));
            if (meta == null) {
                continue;
            }
            IObserver<ObservableEvent> observer = (IObserver<ObservableEvent>) bean;
            ObserverBindingBuilder binding = new ObserverBindingBuilder(observer, this);
            if (meta.events().length > 0) {
                binding.onlyEvents(meta.events());
            }
            if (meta.sources().length > 0) {
                binding.matchingAnySource(meta.sources());
            }
            this.autoDetectedBindings.put(beanClass.getName(), binding);
            log.debug("@Observer bean auto-registered: {} (events={}, sources={})",
                    beanClass.getSimpleName(), meta.events().length, meta.sources().length);
        }
        // NB: there is no @Observable auto-detection by design. The
        // IObservable instances we care about are produced by the engine
        // builders themselves (Workflow, Runtime, Mapper, ScriptContext,
        // …) — not by the DI container. Those builders depend on
        // IObservabilityBuilder and call binding.attachSource(...) directly
        // on the binding they fetch via getBinding(). Users who want to
        // expose an extra observable can either pass it to .observe(...)
        // on this builder or call binding.attachSource(...) post-build.
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

        // Attach every manually-declared source. Engine builders that
        // depend on this builder will attach their own IObservable
        // instances post-build, directly through getBinding().
        for (IObservable src : this.manualObservables.values()) {
            binding.attachSource(src);
        }

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
