package com.garganttua.core.injection;

/**
 * Defines the lifecycle strategy (scope) for beans in the dependency injection system.
 *
 * <p>
 * Bean strategies control how bean instances are managed and shared within the DI context.
 * The strategy determines whether a bean is shared across the application (singleton) or
 * if a new instance is created for each injection point (prototype). This is analogous to
 * bean scopes in frameworks like Spring or Jakarta CDI.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define a singleton bean
 * IDiContextBuilder contextBuilder = ...;
 * contextBuilder.beanProvider("default")
 *     .withBean(DatabaseConnection.class)
 *         .strategy(BeanStrategy.singleton)
 *         .and()
 *     .and();
 *
 * // Define a prototype bean
 * contextBuilder.beanProvider("default")
 *     .withBean(RequestHandler.class)
 *         .strategy(BeanStrategy.prototype)
 *         .and()
 *     .and();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see BeanDefinition
 * @see IBeanFactory
 */
public enum BeanStrategy {

    /**
     * Singleton strategy - one shared instance per context.
     *
     * <p>
     * When a bean is marked as singleton, the DI context creates and manages a single
     * instance that is shared across all injection points. The instance is created on
     * first access (lazy initialization) or during context initialization, and is reused
     * for all subsequent requests. This is the default strategy for most beans and is
     * appropriate for stateless services, shared resources, and configuration objects.
     * </p>
     *
     * <p>
     * Thread Safety: Singleton beans must be thread-safe as they can be accessed
     * concurrently by multiple threads.
     * </p>
     */
    singleton,

    /**
     * Prototype strategy - new instance for each injection.
     *
     * <p>
     * When a bean is marked as prototype, the DI context creates a new instance every
     * time the bean is requested. Each injection point receives its own dedicated instance,
     * and the context does not manage the lifecycle of these instances. This strategy is
     * appropriate for stateful objects, request-scoped data, and beans that should not
     * be shared.
     * </p>
     *
     * <p>
     * Memory Consideration: Prototype beans are not cached, so frequent creation may
     * impact performance. Use this strategy only when instance isolation is required.
     * </p>
     */
    prototype

}
