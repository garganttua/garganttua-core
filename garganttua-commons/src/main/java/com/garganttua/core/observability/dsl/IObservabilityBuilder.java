package com.garganttua.core.observability.dsl;

import com.garganttua.core.dsl.IAutomaticBuilder;
import com.garganttua.core.dsl.IObservableBuilder;
import com.garganttua.core.dsl.IPackageableBuilder;
import com.garganttua.core.dsl.dependency.IDependentBuilder;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;
import com.garganttua.core.reflection.annotations.Reflected;

/**
 * Root builder for declaring {@link IObserver}s with optional per-subscription
 * filtering. Sources ({@link IObservable}) are <strong>not</strong> declared
 * here — instead, engine builders that produce observable engines declare a
 * dependency on this builder and self-register their built engine with the
 * resulting {@link ObservabilityBinding} via
 * {@link ObservabilityBinding#attachSource(IObservable)}.
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
 * <p>Typical usage with the dependency-inversion model:
 * <pre>{@code
 * IObservabilityBuilder obs = ObservabilityBuilder.create()
 *         .observer(adHocObserver).onlyEvents(ErrorEvent.class).up()
 *         .autoDetect(true);
 *
 * // Each engine builder declares the obs builder as a dependency. After it
 * // finishes building its engine, it auto-attaches the result to the binding.
 * Workflow wf = WorkflowBuilder.create().provide(obs)....build();
 * Mapper   m  = MapperBuilder.create().provide(obs)....build();
 *
 * // The binding now holds (wf -> wrapper), (m -> wrapper) registrations.
 * try (var binding = obs.build()) {
 *     wf.execute(...);
 *     m.map(...);
 * }   // detaches every observer
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
@Reflected
public interface IObservabilityBuilder
        extends IAutomaticBuilder<IObservabilityBuilder, ObservabilityBinding>,
                IObservableBuilder<IObservabilityBuilder, ObservabilityBinding>,
                IDependentBuilder<IObservabilityBuilder, ObservabilityBinding>,
                IPackageableBuilder<IObservabilityBuilder, ObservabilityBinding> {

    /**
     * Registers a new observer subscription. The returned linked builder lets
     * the caller refine the filter, then return here via
     * {@link IObserverBindingBuilder#up()}.
     *
     * <p>Distinct from the inherited {@link IObservableBuilder#observer} which
     * registers a build-time callback fired when this builder's own
     * {@link #build()} completes.
     */
    IObserverBindingBuilder subscribe(IObserver<ObservableEvent> observer);

    /**
     * Declares one or more {@link IObservable} sources that this builder
     * must attach to its {@link ObservabilityBinding} during {@link #build()}.
     *
     * <p>This is the manual counterpart to {@code @Observable}-annotated
     * beans (which the builder discovers automatically through the
     * {@link com.garganttua.core.injection.IInjectionContext}). Both paths
     * are complementary, and post-build {@link ObservabilityBinding#attachSource}
     * calls (used by engine builders such as {@code WorkflowBuilder},
     * {@code RuntimeBuilder}, {@code Bootstrap}) still work in parallel.
     */
    IObservabilityBuilder observe(IObservable... sources);

    /**
     * @return the binding produced by this builder once {@link #build()} has
     *         been invoked; otherwise {@code null}. Engine builders that
     *         declare this builder as a dependency use this accessor to push
     *         their built {@link IObservable} into the binding via
     *         {@link ObservabilityBinding#attachSource(IObservable)}.
     */
    ObservabilityBinding getBinding();
}
