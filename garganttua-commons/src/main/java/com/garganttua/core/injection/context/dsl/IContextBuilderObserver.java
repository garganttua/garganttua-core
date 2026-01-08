package com.garganttua.core.injection.context.dsl;

import com.garganttua.core.injection.IInjectionContext;

/**
 * Functional interface for observing and reacting to DI context building events.
 *
 * <p>
 * {@code IContextBuilderObserver} defines a callback that is invoked during the DI context
 * build process, allowing custom logic to be executed when the context is constructed. This
 * enables integration hooks, validation, logging, or post-configuration steps during context
 * initialization. Observers are registered with {@link IInjectionContextBuilder#observer(IContextBuilderObserver)}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a custom observer for logging
 * IContextBuilderObserver loggingObserver = (context) -> {
 *     System.out.println("DI Context built successfully");
 *     System.out.println("Total bean providers: " + context.getBeanProviders().size());
 * };
 *
 * // Register observer during context building
 * IInjectionContext context = InjectionContextBuilder.create()
 *     .withPackage("com.myapp")
 *     .observer(loggingObserver)
 *     .observer((ctx) -> {
 *         // Additional validation
 *         if (ctx.queryBean(DataSource.class).isEmpty()) {
 *             throw new DiException("DataSource bean not found");
 *         }
 *     })
 *     .build();
 *
 * // Observer for metrics collection
 * IContextBuilderObserver metricsObserver = (context) -> {
 *     MetricsRegistry.recordContextSize(context.getBeanProviders().size());
 *     MetricsRegistry.recordInitTime(System.currentTimeMillis());
 * };
 * }</pre>
 *
 * <h2>Use Cases</h2>
 * <ul>
 * <li>Logging and debugging context initialization</li>
 * <li>Validating required beans are present</li>
 * <li>Collecting metrics and statistics</li>
 * <li>Post-configuration setup and initialization</li>
 * <li>Integration with external monitoring systems</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see IInjectionContextBuilder#observer(IContextBuilderObserver)
 * @see IInjectionContext
 */
@FunctionalInterface
public interface IContextBuilderObserver {

    /**
     * Handles a context building event.
     *
     * <p>
     * This method is invoked when the DI context has been built and configured.
     * The provided context is fully initialized and ready for use. Implementations
     * can inspect, validate, or enhance the context.
     * </p>
     *
     * @param context the newly built DI context
     */
    void handle(IInjectionContext context);
}
