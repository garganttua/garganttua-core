package com.garganttua.core.observability;

import java.time.Instant;
import java.util.UUID;

/**
 * A point-in-time log record emitted within an observable execution.
 * <p>
 * Unlike {@link StartEvent}/{@link EndEvent}/{@link ErrorEvent}, a log event does
 * not bracket a unit of work and carries no duration — it is a standalone message
 * correlated with the surrounding execution via {@link #executionId()}.
 * <ul>
 *   <li>{@code level} — severity of the message (see {@link Level}).</li>
 *   <li>{@code message} — the human-readable log text, or {@code null}.</li>
 *   <li>{@code payload} — an optional arbitrary domain object attached to the
 *       record (the data the message is about), or {@code null}.</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA02
 */
public record LogEvent(UUID executionId, Instant timestamp, String source,
		Level level, String message, Object payload) implements ObservableEvent {

	/**
	 * Severity levels, aligned with the conventional SLF4J/Log4j ladder.
	 */
	public enum Level {
		TRACE, DEBUG, INFO, WARN, ERROR
	}

	/**
	 * Convenience constructor for a message without a payload.
	 */
	public LogEvent(UUID executionId, Instant timestamp, String source, Level level, String message) {
		this(executionId, timestamp, source, level, message, null);
	}
}
