package com.garganttua.core.observability;

/**
 * A component that can be observed by {@link IObserver} instances.
 *
 * @param <E> the event type fired by this observable
 * @since 2.0.0-ALPHA02
 */
public interface IObservable<E extends ObservableEvent> {

	void addObserver(IObserver<E> observer);

	void removeObserver(IObserver<E> observer);
}
