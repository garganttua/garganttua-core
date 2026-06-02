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

	/**
	 * Register an observer to receive every {@link ObservableEvent} this source
	 * subsequently emits.
	 *
	 * @param observer the observer to attach
	 */
	void addObserver(IObserver<ObservableEvent> observer);

	/**
	 * Detach a previously registered observer; a no-op if it was never attached.
	 *
	 * @param observer the observer to remove
	 */
	void removeObserver(IObserver<ObservableEvent> observer);
}
