package com.garganttua.core.observability.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.garganttua.core.bootstrap.annotations.Bootstrap;
import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.dsl.dependency.AbstractAutomaticDependentBuilder;
import com.garganttua.core.dsl.dependency.DependencySpec;
import com.garganttua.core.injection.BeanReference;
import com.garganttua.core.injection.DiException;
import com.garganttua.core.injection.IInjectionContext;
import com.garganttua.core.injection.context.dsl.IInjectionContextBuilder;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;
import com.garganttua.core.reflection.annotations.Reflected;

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
 * <p>Declares a {@link DependencyPhase#BUILD BUILD}-phase optional dependency
 * on {@link IInjectionContextBuilder} — when present, {@link Observer @Observer}
 * classes are resolved as beans from the {@link IInjectionContext} (so they
 * can carry their own DI requirements). When absent, they are instantiated
 * via the global {@link IReflection} provider (no-arg ctor required).
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
            DependencySpec.use(IClass.getClass(IInjectionContextBuilder.class)));

    private final List<ObserverBindingBuilder> bindings = new ArrayList<>();
    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());
    private final Set<IBuilderObserver<IObservabilityBuilder, ObservabilityBinding>> buildObservers = new HashSet<>();
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
        this.bindings.add(binding);
        return binding;
    }

    @Override
    public ObservabilityBinding getBinding() {
        return this.built;
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
        if (!(dependency instanceof IInjectionContext context)) {
            return;
        }
        IReflection reflection = getReflection();
        if (reflection == null) {
            log.warn("autoDetect(true) requested but no IReflection is installed — @Observer scan skipped");
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
            Object instance = resolveObserverInstance(klass, reflection, context);
            if (instance == null) {
                continue;
            }
            if (!(instance instanceof IObserver<?>)) {
                log.warn("@Observer class {} does not implement IObserver — skipping",
                        klass.getName());
                continue;
            }
            IObserver<ObservableEvent> observer = (IObserver<ObservableEvent>) instance;
            IObserverBindingBuilder binding = this.subscribe(observer);
            if (meta.events().length > 0) {
                binding.onlyEvents(meta.events());
            }
            if (meta.sources().length > 0) {
                binding.matchingAnySource(meta.sources());
            }
            binding.up();
            log.debug("@Observer auto-registered: {} (events={}, sources={})",
                    klass.getSimpleName(), meta.events().length, meta.sources().length);
        }
    }

    /**
     * Prefer DI lookup, then fall back to direct {@code newInstance}. Returns
     * {@code null} if neither path works (a warning is logged).
     */
    private Object resolveObserverInstance(IClass<?> klass, IReflection reflection,
            IInjectionContext context) {
        try {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            BeanReference ref = new BeanReference(klass, Optional.empty(), Optional.empty(), Set.of());
            @SuppressWarnings("unchecked")
            Optional<Object> bean = context.queryBean(ref);
            if (bean.isPresent()) {
                return bean.get();
            }
        } catch (DiException e) {
            log.debug("DI lookup for @Observer {} failed, falling back to reflection: {}",
                    klass.getName(), e.getMessage());
        }
        try {
            return reflection.newInstance(klass);
        } catch (ReflectionException e) {
            log.warn("@Observer class {} could not be instantiated (no-arg ctor required, no matching bean): {}",
                    klass.getName(), e.getMessage());
            return null;
        }
    }

    @Override
    protected ObservabilityBinding doBuild() throws DslException {
        List<IObserver<ObservableEvent>> wrappers = new ArrayList<>(this.bindings.size());
        for (ObserverBindingBuilder b : this.bindings) {
            wrappers.add(b.buildWrapper());
        }
        ObservabilityBinding binding = new ObservabilityBinding(wrappers);
        this.built = binding;
        for (IBuilderObserver<IObservabilityBuilder, ObservabilityBinding> o : this.buildObservers) {
            try {
                o.handle(binding);
            } catch (RuntimeException e) {
                log.warn("Build observer threw: {}", e.getMessage());
            }
        }
        return binding;
    }

    @Override
    protected void doPreBuildWithDependency(Object dependency) {
        // No-op
    }

    @Override
    protected void doPostBuildWithDependency(Object dependency) {
        // No-op
    }
}
