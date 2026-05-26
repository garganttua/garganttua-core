package com.garganttua.core.observability;

import java.util.UUID;

/**
 * Thread-local accessor for the current observability session.
 * <p>
 * An engine that fires observability events pushes a session — a registry and
 * an {@link UUID} execution identifier — before invoking nested work, and
 * restores the previous session in {@code finally}. The
 * {@code ObservabilityExpressions.observe} expression function and Java-side
 * emitters both read the session via {@link #current()}.
 * <p>
 * The holder is <strong>stackable</strong>: {@link #push} returns the previous
 * session (possibly {@code null}) and callers must pass that value to
 * {@link #pop(Session)} so nested engine invocations (workflow → script →
 * runtime → step) do not lose the outer correlation. Always pair them with
 * try/finally:
 * <pre>{@code
 * Session previous = ObservableContextHolder.push(registry, uuid);
 * try {
 *     // ... do work that may emit events ...
 * } finally {
 *     ObservableContextHolder.pop(previous);
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservableContextHolder {

	private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

	private ObservableContextHolder() {
	}

	/**
	 * Bind a new session to the current thread and return the previously bound
	 * session (or {@code null}). The caller must pass the returned value to
	 * {@link #pop(Session)} in a {@code finally} block.
	 */
	public static Session push(ObservableRegistry registry, UUID executionId) {
		Session previous = CURRENT.get();
		CURRENT.set(new Session(registry, executionId));
		return previous;
	}

	/**
	 * @return the session currently bound to this thread, or {@code null}.
	 */
	public static Session current() {
		return CURRENT.get();
	}

	/**
	 * Restore the session that was active before the matching {@link #push}.
	 * Passing {@code null} clears the binding entirely (the outermost frame).
	 */
	public static void pop(Session previous) {
		if (previous == null) {
			CURRENT.remove();
		} else {
			CURRENT.set(previous);
		}
	}

	/**
	 * A bound (registry, executionId) pair active for one logical execution on
	 * the current thread.
	 */
	public record Session(ObservableRegistry registry, UUID executionId) {
	}
}
