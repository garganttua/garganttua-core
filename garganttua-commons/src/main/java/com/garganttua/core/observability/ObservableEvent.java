package com.garganttua.core.observability;

import java.time.Instant;
import java.util.UUID;

/**
 * Sealed root of the observability event hierarchy.
 * <p>
 * All observable events share four identity fields:
 * <ul>
 *   <li>{@link #executionId()} — a UUID correlating every event emitted by a
 *       single logical execution (one workflow run, one mapping, one operation).</li>
 *   <li>{@link #timestamp()} — the wall-clock instant the event was emitted.</li>
 *   <li>{@link #source()} — a stable, hierarchical string identifier of the unit
 *       producing the event, e.g. {@code "workflow:users:update"} or
 *       {@code "script:business.CREATE_ONE"}.</li>
 *   <li>{@link #payload()} — an optional arbitrary domain object the emitter
 *       chose to attach to the event (e.g. the input or output of the observed
 *       unit of work), or {@code null} when none was provided.</li>
 * </ul>
 * Permitted implementations: {@link StartEvent}, {@link EndEvent},
 * {@link ErrorEvent}, {@link LogEvent}.
 *
 * @since 2.0.0-ALPHA02
 */
public sealed interface ObservableEvent permits StartEvent, EndEvent, ErrorEvent, LogEvent {

	/**
	 * @return the UUID correlating every event emitted by a single logical
	 *         execution.
	 */
	UUID executionId();

	/**
	 * @return the wall-clock instant the event was emitted.
	 */
	Instant timestamp();

	/**
	 * @return the stable, hierarchical identifier of the unit producing the event.
	 */
	String source();

	/**
	 * @return an optional arbitrary domain object attached to this event, or
	 *         {@code null} when the emitter did not provide one.
	 */
	Object payload();
}
