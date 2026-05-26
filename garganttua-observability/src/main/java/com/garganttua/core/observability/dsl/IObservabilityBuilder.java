package com.garganttua.core.observability.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;

/**
 * Root builder for wiring one or more {@link IObserver}s to one or more
 * {@link IObservable}s with optional per-subscription filtering.
 *
 * <p>The builder participates in the framework's standard auto-detection
 * conventions:
 * <ul>
 *   <li>{@link #autoDetect(boolean) autoDetect(true)} enables the scan of
 *       {@code @Observer}-annotated classes during {@link #build()}.</li>
 *   <li>{@link #withPackage(String)} / {@link #withPackages(String[])} narrow
 *       the scan to one or more specific packages. Without any package,
 *       autoDetect scans the whole classpath through the globally-installed
 *       {@link com.garganttua.core.reflection.IAnnotationScanner}.</li>
 * </ul>
 *
 * <p>Typical usage:
 * <pre>{@code
 * try (var binding = ObservabilityBuilder.create()
 *         .observe(workflow, mapper, runtime)
 *         .withPackage("com.myapp.observers")
 *         .autoDetect(true)
 *         .observer(adHocObserver)
 *             .onlyEvents(ErrorEvent.class)
 *         .up()
 *         .build()) {
 *     // … run the engines …
 * }   // binding.close() detaches every observer
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IObservabilityBuilder
        extends IAutomaticBuilder<IObservabilityBuilder, ObservabilityBinding>,
                IPackageableBuilder<IObservabilityBuilder, ObservabilityBinding> {

    /**
     * Declares the default set of observables that subsequently-registered
     * observers will subscribe to. An observer can narrow this default by
     * calling {@link IObserverBindingBuilder#toObservable(IObservable...)}.
     *
     * @param sources one or more observables (no nulls)
     * @return this builder for chaining
     */
    IObservabilityBuilder observe(IObservable<? extends ObservableEvent>... sources);

    /**
     * Registers a new observer subscription. The returned linked builder lets
     * the caller refine the filter and (optionally) override the observable
     * set, then return here via {@link IObserverBindingBuilder#up()}.
     */
    IObserverBindingBuilder observer(IObserver<ObservableEvent> observer);
}
