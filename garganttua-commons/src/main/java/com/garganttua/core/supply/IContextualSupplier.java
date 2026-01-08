package com.garganttua.core.supply;

import java.util.Optional;

/**
 * Contextual supplier interface for providing object instances based on runtime
 * context.
 *
 * <p>
 * {@code IContextualSupplier} extends {@link ISupplier} to support
 * context-aware
 * object creation. This is essential for dependency injection scenarios where
 * the created
 * object depends on runtime information such as the owning container, request
 * scope, or
 * parent objects. The supplier requires a primary context (owner) and
 * optionally accepts
 * additional contexts for complex resolution scenarios.
 * </p>
 *
 * <h2>Usage Example</h2>
 * 
 * <pre>
 * {@code
 * // Supplier that requires a InjectionContext to resolve dependencies
 * IContextualSupplier<UserService, InjectionContext> supplier =
 *     new IContextualSupplier<>() {
 *         &#64;Override
 *         public Class<InjectionContext> getOwnerContextType() {
 *             return InjectionContext.class;
 *         }
 *
 *         &#64;Override
 *         public Optional<UserService> supply(InjectionContext context, Object... otherContexts)
 *                 throws SupplyException {
 *             // Resolve dependencies from context
 *             UserRepository repo = context.getBean(UserRepository.class);
 *             EmailService email = context.getBean(EmailService.class);
 *             return Optional.of(new UserService(repo, email));
 *         }
 *
 *         @Override
 *         public Type getSuppliedType() {
 *             return UserService.class;
 *         }
 *     };
 *
 * // Usage with context
 * InjectionContext context = ...;
 * Optional<UserService> service = supplier.supply(context);
 * }
 * </pre>
 *
 * <h2>Context Resolution</h2>
 * <p>
 * The {@link Supplier#contextualSupply(ISupplier, Object...)} utility method
 * automatically matches the required context type from the provided contexts
 * array,
 * simplifying context-aware instantiation.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends on the implementation and the provided contexts. Ensure
 * that context objects are thread-safe if the supplier is used concurrently.
 * </p>
 *
 * @param <Supplied> the type of object this supplier provides
 * @param <Context>  the type of the required owner context
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 * @see Supplier#contextualSupply(ISupplier, Object...)
 */
public interface IContextualSupplier<Supplied, Context> extends ISupplier<Supplied> {

    /**
     * Throws an exception indicating that context is required.
     *
     * <p>
     * This default implementation ensures that contextual suppliers cannot be used
     * without providing the required context. Callers must use
     * {@link #supply(Object, Object...)} instead.
     * </p>
     *
     * @return never returns normally
     * @throws SupplyException always, indicating that context is required
     */
    @Override
    default Optional<Supplied> supply() throws SupplyException {
        if (getOwnerContextType() != Void.class)
            throw new SupplyException(
                    "Owner context of type " + getOwnerContextType().getSimpleName() + " required for this supplier");

        return supply(null);
    }

    /**
     * Returns the required owner context type for this supplier.
     *
     * <p>
     * This method declares the primary context type required for object creation.
     * The framework uses this information to match and provide the appropriate
     * context at runtime.
     * </p>
     *
     * @return the {@link Class} object representing the required context type
     */
    Class<Context> getOwnerContextType();

    /**
     * Supplies an instance using the provided owner context and optional additional
     * contexts.
     *
     * <p>
     * This method creates or retrieves an instance using the primary owner context
     * and any additional contexts that may be needed for complex resolution
     * scenarios.
     * The implementation can use the contexts to resolve dependencies, access
     * configuration, or determine creation strategies.
     * </p>
     *
     * @param ownerContext  the primary context required for object creation (never
     *                      {@code null})
     * @param otherContexts additional optional contexts that may assist in object
     *                      creation
     * @return an {@link Optional} containing the supplied instance, or empty if
     *         unavailable
     * @throws SupplyException if an error occurs during instance creation or
     *                         context resolution
     */
    Optional<Supplied> supply(Context ownerContext, Object... otherContexts) throws SupplyException;

    default boolean isContextual() {
        return true;
    }

}
