package com.garganttua.core.observability;

import java.util.UUID;

/**
 * Thread-local accessor for the current observability session.
 * <p>
 * An engine that fires observability events from script-side code pushes a
 * session — a registry and an {@link UUID} execution identifier — before
 * invoking the script, and pops it in {@code finally}. The
 * {@link ObservabilityExpressions#observe} expression function reads the
 * session via {@link #current()}.
 * <p>
 * Direct Java callers should not use this holder; pass the registry and
 * execution id explicitly instead.
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservableContextHolder {

	private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

	private ObservableContextHolder() {
	}

	public static void push(ObservableRegistry<ObservableEvent> registry, UUID executionId) {
		CURRENT.set(new Session(registry, executionId));
	}

	public static Session current() {
		return CURRENT.get();
	}

	public static void pop() {
		CURRENT.remove();
	}

	/**
	 * A bound (registry, executionId) pair active for one logical execution on
	 * the current thread.
	 */
	public record Session(ObservableRegistry<ObservableEvent> registry, UUID executionId) {
	}
}
