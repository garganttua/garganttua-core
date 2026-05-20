package com.garganttua.core.observability;

import java.time.Instant;
import java.util.UUID;

/**
 * Sealed root of the observability event hierarchy.
 * <p>
 * All observable events share three identity fields:
 * <ul>
 *   <li>{@link #executionId()} — a UUID correlating every event emitted by a
 *       single logical execution (one workflow run, one mapping, one operation).</li>
 *   <li>{@link #timestamp()} — the wall-clock instant the event was emitted.</li>
 *   <li>{@link #source()} — a stable, hierarchical string identifier of the unit
 *       producing the event, e.g. {@code "workflow:users:update"} or
 *       {@code "script:business.CREATE_ONE"}.</li>
 * </ul>
 * Permitted implementations: {@link StartEvent}, {@link EndEvent}, {@link ErrorEvent}.
 *
 * @since 2.0.0-ALPHA02
 */
public sealed interface ObservableEvent permits StartEvent, EndEvent, ErrorEvent {

	UUID executionId();

	Instant timestamp();

	String source();
}
