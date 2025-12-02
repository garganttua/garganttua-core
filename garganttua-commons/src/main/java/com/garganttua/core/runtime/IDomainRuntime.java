package com.garganttua.core.runtime;

/**
 * Marker interface for domain-specific runtimes.
 *
 * <p>
 * IDomainRuntime serves as a type marker for runtime implementations that belong
 * to a specific business domain. This interface is used to categorize and identify
 * runtimes within a domain-driven architecture.
 * </p>
 *
 * <p>
 * Implementations typically represent domain-specific workflows or processes,
 * such as order processing, user management, or inventory control. The marker
 * allows for domain-based filtering and organization of runtimes.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * @RuntimeDefinition(input = Order.class, output = OrderResult.class)
 * public class OrderProcessingRuntime implements IDomainRuntime {
 *     // Domain-specific runtime implementation
 * }
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 */
public interface IDomainRuntime {

}
