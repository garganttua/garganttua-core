package com.garganttua.core.reflection.binders;

/**
 * Context-aware constructor binder for object instantiation with runtime dependency resolution.
 *
 * <p>
 * {@code IContextualConstructorBinder} combines {@link IConstructorBinder} and
 * {@link IContextualExecutableBinder} to enable object construction where constructor
 * parameters are resolved from a runtime context. This is the cornerstone of
 * constructor-based dependency injection, allowing objects to be instantiated with
 * dependencies supplied by a DI container or other context provider.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Constructor requiring dependencies from DiContext
 * IContextualConstructorBinder<UserService> constructor =
 *     ContextualConstructorBinder
 *         .forClass(UserService.class)
 *         .withParameter(UserRepository.class)  // Resolved from context
 *         .withParameter(EmailService.class)    // Resolved from context
 *         .withParameter(Logger.class)          // Resolved from context
 *         .build();
 *
 * // Execute with DiContext
 * DiContext context = ...;
 * Optional<UserService> service = constructor.execute(context);
 *
 * // Check dependencies before instantiation
 * Set<Class<?>> deps = constructor.getDependencies();
 * // Returns { UserRepository.class, EmailService.class, Logger.class }
 *
 * for (Class<?> dep : deps) {
 *     if (!context.hasBean(dep)) {
 *         throw new Exception("Missing dependency: " + dep.getName());
 *     }
 * }
 * }</pre>
 *
 * <h2>Context Type Specialization</h2>
 * <p>
 * While {@link IContextualExecutableBinder} supports typed owner contexts,
 * {@code IContextualConstructorBinder} uses {@link Void} as the owner context
 * type by default, indicating that constructors don't have an owning instance
 * context. Instead, all parameters are resolved from the provided context array.
 * </p>
 *
 * <h2>Dependency Injection Pattern</h2>
 * <p>
 * This interface is fundamental to constructor-based dependency injection:
 * <ol>
 *   <li>Analyze constructor parameters to determine dependencies</li>
 *   <li>Validate that all dependencies are available in the context</li>
 *   <li>Resolve each dependency from the context</li>
 *   <li>Invoke the constructor with resolved parameters</li>
 *   <li>Return the newly created instance</li>
 * </ol>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Constructor binders are typically thread-safe if configured with immutable
 * settings. Each invocation creates a new instance, making the binder stateless
 * and safe for concurrent use. However, thread safety of the constructed objects
 * depends on their own implementation.
 * </p>
 *
 * @param <Constructed> the type of object this constructor creates
 * @since 2.0.0-ALPHA01
 * @see IConstructorBinder
 * @see IContextualExecutableBinder
 */
public interface IContextualConstructorBinder<Constructed> extends IConstructorBinder<Constructed>, IContextualExecutableBinder<Constructed, Void> {

    /**
     * Returns {@link Void}.class as the owner context type.
     *
     * <p>
     * Constructors don't have an owning instance context (unlike methods which
     * belong to an instance or class). This method returns {@link Void}.class
     * to indicate that no owner context is required, and all parameters are
     * resolved from the contexts array passed to
     * {@link #execute(Object, Object...)}.
     * </p>
     *
     * @return {@link Void}.class, indicating no owner context is required
     */
    @Override
    default Class<Void> getOwnerContextType(){
        return Void.class;
    }

}
