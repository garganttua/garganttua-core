package com.garganttua.core.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.garganttua.core.observability.ObservableContextHolder.Session;

/**
 * Helper that bundles the "fire start/end/error + propagate to parent + manage
 * the ObservableContextHolder session" boilerplate that every observable engine
 * (Runtime, Script, Mapper, Injection, Mutex, Bootstrap, Workflow) needs.
 * <p>
 * Open a scope with {@link #open(ObservableRegistry, UUID)} at the start of an
 * observable unit of work, fire events on it, then close it (typically in a
 * try-with-resources block). When a parent session is active, the scope reuses
 * the parent's registry and execution id so a single observer at the top of the
 * call chain sees correlated events from every engine. Events are also fired to
 * the engine's local registry so direct listeners are never starved.
 *
 * <pre>{@code
 * try (var scope = ObservabilityEmitter.open(observers, uuid)) {
 *     scope.fireStart("runtime:" + name);
 *     try {
 *         // ... do work ...
 *         scope.fireEnd("runtime:" + name, code);
 *     } catch (Throwable t) {
 *         scope.fireError("runtime:" + name, t);
 *         throw t;
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA02
 */
public final class ObservabilityEmitter {

	private ObservabilityEmitter() {
	}

	/**
	 * Open an emission scope. If a parent session is already bound on the
	 * current thread, the scope reuses the parent's registry and execution id;
	 * otherwise it pushes a new session backed by {@code localRegistry} and
	 * {@code localExecutionId}. The returned scope MUST be closed (preferably
	 * via try-with-resources) so the holder stack is restored.
	 */
	public static Scope open(ObservableRegistry localRegistry, UUID localExecutionId) {
		Session parent = ObservableContextHolder.current();
		if (parent != null) {
			return new Scope(parent.registry(), parent.executionId(), localRegistry, null, false);
		}
		Session previous = ObservableContextHolder.push(localRegistry, localExecutionId);
		return new Scope(localRegistry, localExecutionId, localRegistry, previous, true);
	}

	/**
	 * Open a passive scope that piggy-backs on the parent session if one exists.
	 * Used by nested units of work (e.g. an individual runtime step) that do not
	 * own a registry but want to emit Start/End/Error correlated with the
	 * enclosing execution. When no parent session is bound, the scope is a
	 * no-op (fire methods silently drop events). Always close it.
	 */
	public static Scope joinCurrent() {
		Session parent = ObservableContextHolder.current();
		if (parent == null) {
			return new Scope(null, null, null, null, false);
		}
		return new Scope(parent.registry(), parent.executionId(), null, null, false);
	}

	/**
	 * Active emission scope. Closing it pops the holder session it pushed (no-op
	 * when nested under a parent). Fire methods are no-ops when no observer is
	 * registered on either the active or the local registry.
	 */
	public static final class Scope implements AutoCloseable {

		private final ObservableRegistry active;
		private final UUID executionId;
		private final ObservableRegistry local;
		private final Session previous;
		private final boolean pushed;
		private final Instant startedAt;

		private Scope(ObservableRegistry active, UUID executionId,
				ObservableRegistry local, Session previous, boolean pushed) {
			this.active = active;
			this.executionId = executionId;
			this.local = local;
			this.previous = previous;
			this.pushed = pushed;
			this.startedAt = Instant.now();
		}

		/**
		 * @return the execution id this scope correlates events with (inherited
		 *         from the parent session when nested), or {@code null} for a
		 *         no-op scope.
		 */
		public UUID executionId() {
			return executionId;
		}

		/**
		 * @return the instant this scope was opened, used as the start point for
		 *         {@code End}/{@code Error} durations.
		 */
		public Instant startedAt() {
			return startedAt;
		}

		/**
		 * @return whether any observer is registered on the active or local
		 *         registry; fire methods short-circuit when this is {@code false}.
		 */
		public boolean hasObservers() {
			return (active != null && active.hasObservers()) || (local != null && local != active && local.hasObservers());
		}

		/**
		 * Fire a {@link StartEvent} with no payload.
		 *
		 * @param source the hierarchical source identifier
		 */
		public void fireStart(String source) {
			fireStart(source, null);
		}

		/**
		 * Fire a {@link StartEvent}. No-op when no observer is listening.
		 *
		 * @param source  the hierarchical source identifier
		 * @param payload optional domain object to attach, or {@code null}
		 */
		public void fireStart(String source, Object payload) {
			if (!hasObservers()) {
				return;
			}
			fire(new StartEvent(executionId, Instant.now(), source, payload));
		}

		/**
		 * Fire an {@link EndEvent} with no status code or payload.
		 *
		 * @param source the hierarchical source identifier
		 */
		public void fireEnd(String source) {
			fireEnd(source, null, null);
		}

		/**
		 * Fire an {@link EndEvent} carrying a domain status code.
		 *
		 * @param source the hierarchical source identifier
		 * @param code   optional domain status code, or {@code null}
		 */
		public void fireEnd(String source, Integer code) {
			fireEnd(source, code, null);
		}

		/**
		 * Fire an {@link EndEvent} with the elapsed duration since {@link #startedAt()}.
		 * No-op when no observer is listening.
		 *
		 * @param source  the hierarchical source identifier
		 * @param code    optional domain status code, or {@code null}
		 * @param payload optional domain object to attach, or {@code null}
		 */
		public void fireEnd(String source, Integer code, Object payload) {
			if (!hasObservers()) {
				return;
			}
			Instant now = Instant.now();
			Duration duration = Duration.between(startedAt, now);
			fire(new EndEvent(executionId, now, source, duration, code, payload));
		}

		/**
		 * Fire an {@link ErrorEvent} with no payload.
		 *
		 * @param source  the hierarchical source identifier
		 * @param failure the throwable that ended the unit of work
		 */
		public void fireError(String source, Throwable failure) {
			fireError(source, failure, null);
		}

		/**
		 * Fire an {@link ErrorEvent} with the elapsed duration since {@link #startedAt()}.
		 * No-op when no observer is listening.
		 *
		 * @param source  the hierarchical source identifier
		 * @param failure the throwable that ended the unit of work
		 * @param payload optional domain object to attach, or {@code null}
		 */
		public void fireError(String source, Throwable failure, Object payload) {
			if (!hasObservers()) {
				return;
			}
			Instant now = Instant.now();
			Duration duration = Duration.between(startedAt, now);
			fire(new ErrorEvent(executionId, now, source, duration, failure, payload));
		}

		/**
		 * Fire a {@link LogEvent} with no payload.
		 *
		 * @param source  the hierarchical source identifier
		 * @param level   the log severity
		 * @param message the log text
		 */
		public void fireLog(String source, LogEvent.Level level, String message) {
			fireLog(source, level, message, null);
		}

		/**
		 * Fire a {@link LogEvent} correlated with this scope's execution. No-op
		 * when no observer is listening.
		 *
		 * @param source  the hierarchical source identifier
		 * @param level   the log severity
		 * @param message the log text
		 * @param payload optional domain object to attach, or {@code null}
		 */
		public void fireLog(String source, LogEvent.Level level, String message, Object payload) {
			if (!hasObservers()) {
				return;
			}
			fire(new LogEvent(executionId, Instant.now(), source, level, message, payload));
		}

		private void fire(ObservableEvent event) {
			if (active != null) {
				active.fire(event);
			}
			if (local != null && local != active) {
				local.fire(event);
			}
		}

		@Override
		public void close() {
			if (pushed) {
				ObservableContextHolder.pop(previous);
			}
		}
	}
}
