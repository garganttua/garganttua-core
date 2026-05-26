package com.garganttua.core.observability.dsl;

import com.garganttua.core.dsl.IBuilder;
import com.garganttua.core.observability.IObservable;
import com.garganttua.core.observability.IObserver;
import com.garganttua.core.observability.ObservabilityBinding;
import com.garganttua.core.observability.ObservableEvent;

/**
 * Root builder for wiring one or more {@link IObserver}s to one or more
 * {@link IObservable}s with optional per-subscription filtering.
 *
 * <p>Typical usage:
 * <pre>{@code
 * import static com.garganttua.core.condition.Conditions.*;
 *
 * try (var binding = ObservabilityBuilder.create()
 *         .observe(workflow, mapper, runtime)
 *         .observer(loggingObserver)
 *             .when(events -> and(
 *                 custom(events, ObservableEvent::source,
 *                     src -> src.startsWith("workflow:")),
 *                 custom(events, e -> e instanceof EndEvent)))
 *         .up()
 *         .observer(errorReporter)
 *             .onlyEvents(ErrorEvent.class)
 *         .up()
 *         .build()) {
 *     // … run the engines …
 * }   // binding.close() detaches both observers
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public interface IObservabilityBuilder extends IBuilder<ObservabilityBinding> {

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
