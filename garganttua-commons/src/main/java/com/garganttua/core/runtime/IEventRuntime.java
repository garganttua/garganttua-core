package com.garganttua.core.runtime;

/**
 * Interface for runtimes that support event publishing.
 *
 * <p>
 * IEventRuntime provides event publishing capabilities for runtime implementations.
 * It allows runtimes to publish events that can be consumed by external observers,
 * enabling decoupled communication and monitoring of runtime execution.
 * </p>
 *
 * <p>
 * This is typically used for integration with event-driven architectures, logging,
 * monitoring systems, or business event processing.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class OrderRuntime implements IRuntime<Order, OrderResult>, IEventRuntime {
 *
 *     @Operation
 *     public OrderResult processOrder(@Input Order order) {
 *         OrderResult result = // ... process order
 *
 *         // Publish event after processing
 *         publishEvent(new OrderProcessedEvent(order.getId()));
 *
 *         return result;
 *     }
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IEvent
 * @see IRuntime
 */
public interface IEventRuntime {

    /**
     * Publishes an event from the runtime.
     *
     * <p>
     * The event will be propagated to all registered event listeners or handlers.
     * This method is typically called during runtime execution to notify external
     * systems of significant events or state changes.
     * </p>
     *
     * @param event the event to publish, must not be null
     * @throws NullPointerException if event is null
     * @see IEvent
     */
    void publishEvent(IEvent event);

}
