package com.garganttua.core.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Marks the successful completion of an observable unit of work.
 * <p>
 * {@code duration} is computed by the registry from the matching {@link StartEvent}
 * when one exists; otherwise it is the elapsed time the emitter chose to report.
 * {@code code} carries an optional domain status (e.g. workflow exit code), or
 * {@code null} when the emitter does not produce one. {@code payload} carries an
 * optional arbitrary domain object (e.g. the output of the unit of work), or
 * {@code null} when the emitter does not provide one.
 *
 * @since 2.0.0-ALPHA02
 */
public record EndEvent(UUID executionId, Instant timestamp, String source,
		Duration duration, Integer code, Object payload) implements ObservableEvent {

	/**
	 * Backward-compatible constructor with no payload.
	 */
	public EndEvent(UUID executionId, Instant timestamp, String source, Duration duration, Integer code) {
		this(executionId, timestamp, source, duration, code, null);
	}
}
