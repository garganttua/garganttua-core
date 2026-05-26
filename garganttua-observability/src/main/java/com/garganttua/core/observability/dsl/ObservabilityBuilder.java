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
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservabilityBinding.Registration;
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

    private final List<IObservable> defaultSources = new ArrayList<>();
    private final List<ObserverBindingBuilder> bindings = new ArrayList<>();
    private final Set<String> packages = Collections.synchronizedSet(new HashSet<>());

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
    public IObservabilityBuilder observe(IObservable... sources) {
        Objects.requireNonNull(sources, "sources");
        for (IObservable src : sources) {
            Objects.requireNonNull(src, "source");
            this.defaultSources.add(src);
        }
        return this;
    }

    @Override
    public IObserverBindingBuilder observer(IObserver<ObservableEvent> observer) {
        Objects.requireNonNull(observer, "observer");
        ObserverBindingBuilder binding = new ObserverBindingBuilder(observer, this);
        this.bindings.add(binding);
        return binding;
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
            IObserverBindingBuilder binding = this.observer(observer);
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
        List<Registration> registrations = new ArrayList<>();
        for (ObserverBindingBuilder b : this.bindings) {
            List<IObservable> sources = b.overrideSources();
            if (sources == null) {
                sources = this.defaultSources;
            }
            if (sources.isEmpty()) {
                throw new DslException(
                        "Observer " + b.target().getClass().getSimpleName()
                                + " has no observable source — call .observe(...) on the root"
                                + " builder or .toObservable(...) on the binding.");
            }
            IObserver<ObservableEvent> wrapper = b.buildWrapper();
            for (IObservable src : sources) {
                src.addObserver(wrapper);
                registrations.add(new Registration(src, wrapper));
            }
        }
        return new ObservabilityBinding(registrations);
    }
}
