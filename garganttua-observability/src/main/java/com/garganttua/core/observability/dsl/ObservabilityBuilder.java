package com.garganttua.core.observability.dsl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.dsl.AbstractAutomaticBuilder;
import com.garganttua.core.dsl.DslException;
import com.garganttua.core.dsl.IBuilderObserver;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.observability.annotations.Observer;
import com.garganttua.core.reflection.IClass;
import com.garganttua.core.reflection.IReflection;
import com.garganttua.core.reflection.ReflectionException;

/**
 * Root entry point of the observability DSL. Use {@link #create()} to obtain
 * a fresh builder, declare the observable sources via {@link #observe},
 * register observers (optionally filtered) via {@link #observer}, optionally
 * enable {@link #autoDetect(boolean)} scoped by {@link #withPackage(String)},
 * then call {@link #build()} to materialize an {@link ObservabilityBinding}.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservabilityBuilder
        extends AbstractAutomaticBuilder<IObservabilityBuilder, ObservabilityBinding>
        implements IObservabilityBuilder {

    private static final IDiagnostic log = Diagnostics.of(ObservabilityBuilder.class);

    private final List<ObserverBindingBuilder> bindings = new ArrayList<>();
    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());
    private final Set<IBuilderObserver<IObservabilityBuilder, ObservabilityBinding>> buildObservers = new HashSet<>();
    private volatile ObservabilityBinding built;

    private ObservabilityBuilder() {
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

    // -- AbstractAutomaticBuilder hooks --------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected void doAutoDetection() throws DslException {
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
}
