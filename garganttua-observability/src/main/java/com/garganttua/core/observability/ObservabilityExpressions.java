package com.garganttua.core.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.garganttua.core.diagnostic.Diagnostics;
import com.garganttua.core.diagnostic.IDiagnostic;
import com.garganttua.core.expression.annotations.Expression;
import com.garganttua.core.observability.ObservableContextHolder.Session;

import jakarta.annotation.Nullable;

import com.garganttua.core.reflection.annotations.Reflected;
/**
 * Script-side instrumentation hook.
 * <p>
 * The {@code :observe(eventType, source[, code])} expression function fires
 * an {@link ObservableEvent} to the registry currently bound on
 * {@link ObservableContextHolder}. The event type is one of:
 * <ul>
 *   <li>{@code "start"} — emits a {@link StartEvent}; the timestamp is recorded
 *       for later duration computation on the matching end event.</li>
 *   <li>{@code "end"} — emits an {@link EndEvent} whose {@code duration} is
 *       computed against the most recent matching start event on the same
 *       {@code source}; {@code code} (third argument) is forwarded if non-null.</li>
 *   <li>{@code "error"} — emits an {@link ErrorEvent}; duration computed as for
 *       end.</li>
 * </ul>
 * No-op when no session is currently bound or the registry has no observers,
 * keeping the cost on unobserved workflows in the low tens of nanoseconds.
 *
 * @since 2.0.0-ALPHA02
 */
@Reflected(queryAllDeclaredMethods = true)
public class ObservabilityExpressions {
    private static final IDiagnostic log = Diagnostics.of(ObservabilityExpressions.class);

	private static final ConcurrentMap<Key, Instant> STARTS = new ConcurrentHashMap<>();

	private ObservabilityExpressions() {
	}

	@Expression(name = "observe", description = "Fire an observability event (start|end|error, source)")
	public static void observe(String eventType, String source) {
		observe(eventType, source, null);
	}

	@Expression(name = "observe", description = "Fire an observability event (start|end|error, source, code)")
	public static void observe(String eventType, String source, @Nullable Integer code) {
		Session session = ObservableContextHolder.current();
		if (session == null) {
			return;
		}
		ObservableRegistry registry = session.registry();
		if (registry == null || !registry.hasObservers()) {
			return;
		}
		Instant now = Instant.now();
		Key key = new Key(session.executionId(), source);
		switch (eventType) {
			case "start" -> {
				STARTS.put(key, now);
				registry.fire(new StartEvent(session.executionId(), now, source));
			}
			case "end" -> {
				Duration duration = computeDuration(key, now);
				registry.fire(new EndEvent(session.executionId(), now, source, duration, code));
			}
			case "error" -> {
				Duration duration = computeDuration(key, now);
				registry.fire(new ErrorEvent(session.executionId(), now, source, duration, null));
			}
			default -> log.warn("Unknown observe() event type: {}", eventType);
		}
	}

	private static Duration computeDuration(Key key, Instant now) {
		Instant start = STARTS.remove(key);
		return (start == null) ? Duration.ZERO : Duration.between(start, now);
	}

	/**
	 * Visible for testing / external lifecycle cleanup.
	 */
	static void clearStarts() {
		STARTS.clear();
	}

	private record Key(java.util.UUID executionId, String source) {
	}
}
