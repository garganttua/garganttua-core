package com.garganttua.core.runtime;

/**
 * Marker interface for runtime events.
 *
 * <p>
 * IEvent serves as the base interface for all event types that can be published
 * during runtime execution. Events are used for decoupled communication between
 * runtime components and external observers.
 * </p>
 *
 * <p>
 * Implementations should extend this interface to define specific event types
 * that carry relevant event data for their domain.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class OrderProcessedEvent implements IEvent {
 *     private final String orderId;
 *     private final Instant timestamp;
 *
 *     public OrderProcessedEvent(String orderId) {
 *         this.orderId = orderId;
 *         this.timestamp = Instant.now();
 *     }
 *
 *     // Getters...
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IEventRuntime
 */
public interface IEvent {

}
