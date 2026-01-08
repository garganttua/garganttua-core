package com.garganttua.core.injection;

/**
 * Factory interface for creating child dependency injection contexts.
 *
 * <p>
 * An {@code IInjectionChildContextFactory} is responsible for instantiating child contexts
 * based on a cloned parent context. This enables hierarchical context structures where
 * child contexts inherit beans and properties from their parent while maintaining
 * their own isolated state. This pattern is useful for request scopes, isolated
 * execution contexts, and modular application structures.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Implement a factory for a custom child context
 * public class RequestScopeContextFactory
 *     implements IInjectionChildContextFactory<RequestScopeContext> {
 *
 *     public RequestScopeContext createChildContext(
 *             IInjectionContext clonedParent,
 *             Object... args) throws DiException {
 *         HttpServletRequest request = (HttpServletRequest) args[0];
 *         return new RequestScopeContext(clonedParent, request);
 *     }
 * }
 *
 * // Register the factory
 * context.registerChildContextFactory(new RequestScopeContextFactory());
 *
 * // Create a child context
 * RequestScopeContext requestContext = context.newChildContext(
 *     RequestScopeContext.class,
 *     httpRequest);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations should be stateless and thread-safe as they may be invoked concurrently.
 * </p>
 *
 * @param <ChildContext> the type of child context this factory creates
 * @since 2.0.0-ALPHA01
 * @see IInjectionContext#newChildContext(Class, Object...)
 * @see IInjectionContext#registerChildContextFactory(IInjectionChildContextFactory)
 */
public interface IInjectionChildContextFactory<ChildContext extends IInjectionContext> {

    /**
     * Creates a new child context based on a cloned parent context.
     *
     * <p>
     * The provided parent context is a clone that has been initialized and started.
     * The factory should use this parent context as the base for the child context,
     * adding any child-specific beans, properties, or configurations.
     * </p>
     *
     * @param clonedParent the initialized and started cloned parent context
     * @param args additional arguments for child context initialization
     * @return the newly created child context
     * @throws DiException if child context creation fails
     */
    ChildContext createChildContext(IInjectionContext clonedParent, Object ...args) throws DiException;
}
