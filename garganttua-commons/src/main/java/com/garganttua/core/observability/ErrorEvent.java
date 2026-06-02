package com.garganttua.core.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Marks the failed completion of an observable unit of work.
 * <p>
 * {@code duration} reports the time elapsed before the failure was raised.
 * {@code payload} carries an optional arbitrary domain object (e.g. the input
 * being processed when the failure occurred), or {@code null} when the emitter
 * does not provide one.
 *
 * @since 2.0.0-ALPHA02
 */
public record ErrorEvent(UUID executionId, Instant timestamp, String source,
		Duration duration, Throwable failure, Object payload) implements ObservableEvent {

	/**
	 * Backward-compatible constructor with no payload.
	 */
	public ErrorEvent(UUID executionId, Instant timestamp, String source, Duration duration, Throwable failure) {
		this(executionId, timestamp, source, duration, failure, null);
	}
}
