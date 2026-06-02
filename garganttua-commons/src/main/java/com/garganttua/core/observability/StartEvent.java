package com.garganttua.core.observability;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks the beginning of an observable unit of work.
 * <p>
 * {@code payload} carries an optional arbitrary domain object (e.g. the input of
 * the unit of work), or {@code null} when the emitter does not provide one.
 *
 * @since 2.0.0-ALPHA02
 */
public record StartEvent(UUID executionId, Instant timestamp, String source, Object payload) implements ObservableEvent {

	/**
	 * Backward-compatible constructor with no payload.
	 */
	public StartEvent(UUID executionId, Instant timestamp, String source) {
		this(executionId, timestamp, source, null);
	}
}
