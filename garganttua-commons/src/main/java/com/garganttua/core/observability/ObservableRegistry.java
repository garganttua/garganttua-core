package com.garganttua.core.observability;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry of observers with built-in exception isolation.
 * <p>
 * Observer iteration is backed by a {@link CopyOnWriteArrayList} so concurrent
 * fires do not see partial mutations. Exceptions thrown by an observer are
 * caught and logged at warn level — a misbehaving observer never breaks the
 * emitter.
 * <p>
 * Use {@link #hasObservers()} to short-circuit expensive event payload
 * construction when no observer is registered.
 *
 * @param <E> the event type managed by this registry
 * @since 2.0.0-ALPHA02
 */
public class ObservableRegistry<E extends ObservableEvent> implements IObservable<E> {
    private static final IDiagnostic log = Diagnostics.of(ObservableRegistry.class);

	private final List<IObserver<E>> observers = new CopyOnWriteArrayList<>();

	@Override
	public void addObserver(IObserver<E> observer) {
		this.observers.add(Objects.requireNonNull(observer, "observer cannot be null"));
	}

	@Override
	public void removeObserver(IObserver<E> observer) {
		this.observers.remove(observer);
	}

	/**
	 * Fire the event to every registered observer. Exceptions thrown by an
	 * observer are caught and logged; processing continues with remaining
	 * observers.
	 */
	public void fire(E event) {
		if (event == null || this.observers.isEmpty()) {
			return;
		}
		for (IObserver<E> observer : this.observers) {
			try {
				observer.onEvent(event);
			} catch (RuntimeException ex) {
				log.warn("Observer {} failed on event {}: {}",
						observer.getClass().getName(), event, ex.getMessage());
			}
		}
	}

	/**
	 * @return {@code true} if at least one observer is currently registered.
	 */
	public boolean hasObservers() {
		return !this.observers.isEmpty();
	}

	/**
	 * @return number of observers currently registered (useful for tests).
	 */
	public int size() {
		return this.observers.size();
	}
}
