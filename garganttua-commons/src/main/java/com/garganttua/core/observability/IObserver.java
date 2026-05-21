package com.garganttua.core.observability;

/**
 * Single-callback observer over a sealed event family.
 * <p>
 * Implementations typically dispatch on the concrete event type with a switch
 * pattern expression — see the module README for an example.
 *
 * @param <E> the event type this observer reacts to
 * @since 2.0.0-ALPHA02
 */
@FunctionalInterface
public interface IObserver<E extends ObservableEvent> {

	void onEvent(E event);
}
