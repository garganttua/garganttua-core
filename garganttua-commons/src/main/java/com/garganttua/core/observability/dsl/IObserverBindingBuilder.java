package com.garganttua.core.observability.dsl;

import java.util.function.Function;
import java.util.function.Predicate;

import com.garganttua.core.condition.dsl.IConditionBuilder;
import com.garganttua.core.dsl.ILinkedBuilder;
import com.garganttua.core.observability.ObservableEvent;

/**
 * Linked sub-builder for one observer registration. Provides the filter DSL
 * for restricting which events reach the wrapped target.
 *
 * <p>Filter methods compose with AND when called multiple times on the same
 * binding (e.g. {@code .onlyEvents(EndEvent.class).matchingSource("workflow:*")}
 * delivers only EndEvents whose source starts with {@code workflow:}).
 *
 * <p>{@link #up()} returns to the parent {@link IObservabilityBuilder} so the
 * caller can register further observers and finally call {@code build()}.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IObserverBindingBuilder extends ILinkedBuilder<IObservabilityBuilder, Void> {

    /**
     * Attach a {@code garganttua-condition}-based filter. The framework hands
     * an {@link EventHolderSupplierBuilder} to the lambda; the user composes a
     * condition that consults the current event through that supplier.
     *
     * <p>The same condition DSL used by {@code RuntimeStepMethodBuilder.condition(...)}
     * is reused here, so users only learn one filtering vocabulary.
     *
     * @param condition lambda that, given an event supplier, returns a condition builder
     * @return this builder for chaining
     */
    IObserverBindingBuilder when(Function<EventHolderSupplierBuilder, IConditionBuilder> condition);

    /**
     * Attach a JDK {@link Predicate} filter. Escape hatch for trivial cases
     * where the condition DSL would be overkill.
     */
    IObserverBindingBuilder where(Predicate<ObservableEvent> predicate);

    /**
     * Restrict delivery to one or more concrete event types.
     */
    @SuppressWarnings("unchecked")
    IObserverBindingBuilder onlyEvents(Class<? extends ObservableEvent>... eventTypes);

    /**
     * Restrict delivery to events whose {@code source()} matches the given
     * glob pattern. {@code *} is the only wildcard supported and matches any
     * substring; everything else is literal. Examples:
     * <ul>
     *   <li>{@code "workflow:*"} matches {@code workflow:foo}, {@code workflow:bar}</li>
     *   <li>{@code "*:critical:*"} matches {@code workflow:critical:bar}</li>
     * </ul>
     */
    IObserverBindingBuilder matchingSource(String globPattern);

    /**
     * Restrict delivery to events whose {@code source()} matches at least ONE
     * of the given glob patterns (OR semantics across patterns; AND with any
     * other filter already attached to this binding). Useful when an
     * annotation or config supplies several patterns at once. An empty array
     * is a no-op (no filter added).
     */
    IObserverBindingBuilder matchingAnySource(String... globPatterns);
}
