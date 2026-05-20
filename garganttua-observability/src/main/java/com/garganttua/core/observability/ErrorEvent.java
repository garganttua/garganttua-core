package com.garganttua.core.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Marks the failed completion of an observable unit of work.
 * <p>
 * {@code duration} reports the time elapsed before the failure was raised.
 *
 * @since 2.0.0-ALPHA02
 */
public record ErrorEvent(UUID executionId, Instant timestamp, String source,
		Duration duration, Throwable failure) implements ObservableEvent {
}
