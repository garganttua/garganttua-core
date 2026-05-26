package com.garganttua.core.observability;

/**
 * A component that can be observed by {@link IObserver} instances.
 *
 * <p>The interface is intentionally non-parametric: every emitter in
 * Garganttua publishes {@link ObservableEvent}s through the common sealed
 * hierarchy ({@link StartEvent}, {@link EndEvent}, {@link ErrorEvent}).
 * Observers dispatch on the concrete subtype via pattern matching at the
 * call site.
 *
 * @since 2.0.0-ALPHA02
 */
public interface IObservable {

	void addObserver(IObserver<ObservableEvent> observer);

	void removeObserver(IObserver<ObservableEvent> observer);
}
