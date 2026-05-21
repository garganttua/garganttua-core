package com.garganttua.core.observability;

import java.time.Instant;
import java.util.UUID;

/**
 * Marks the beginning of an observable unit of work.
 *
 * @since 2.0.0-ALPHA02
 */
public record StartEvent(UUID executionId, Instant timestamp, String source) implements ObservableEvent {
}
