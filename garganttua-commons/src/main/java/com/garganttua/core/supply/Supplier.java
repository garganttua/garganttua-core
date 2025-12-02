package com.garganttua.core.supply;

/**
 * Utility class providing helper methods for working with object suppliers.
 *
 * <p>
 * {@code Supplier} offers static utility methods that simplify supplier usage,
 * particularly for contextual suppliers. The primary use case is automatically
 * matching and providing the correct context to {@link IContextualObjectSupplier}
 * instances, eliminating the need for manual context type checking and casting.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define suppliers
 * IObjectSupplier<Database> dbSupplier = ...;  // Contextual supplier needing DiContext
 * IObjectSupplier<Logger> loggerSupplier = ...; // Simple supplier, no context needed
 *
 * // Available contexts
 * DiContext diContext = ...;
 * HttpRequest request = ...;
 * Object[] contexts = { diContext, request };
 *
 * // Automatic context matching and supply
 * Database db = Supplier.contextualSupply(dbSupplier, contexts);
 * Logger logger = Supplier.contextualSupply(loggerSupplier, contexts);
 * }</pre>
 *
 * <h2>Context Matching</h2>
 * <p>
 * For contextual suppliers, the {@link #contextualSupply(IObjectSupplier, Object...)}
 * method automatically searches the provided contexts array for a compatible context
 * type using {@link Class#isAssignableFrom(Class)}. This supports both exact type
 * matches and subtype relationships.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IObjectSupplier
 * @see IContextualObjectSupplier
 */
public class Supplier {

    /**
     * Supplies an object from the given supplier, automatically matching contexts if needed.
     *
     * <p>
     * This method provides a unified interface for working with both contextual and
     * non-contextual suppliers. For {@link IContextualObjectSupplier} instances, it
     * automatically searches the provided contexts array for a compatible context type
     * and delegates to {@link IContextualObjectSupplier#supply(Object, Object...)}.
     * For standard {@link IObjectSupplier} instances, it simply calls {@link IObjectSupplier#supply()}.
     * </p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If supplier is contextual: Searches for matching context, throws exception if not found</li>
     *   <li>If supplier is non-contextual: Calls {@code supply()} directly, ignoring contexts</li>
     *   <li>Returns {@code null} if the supplier returns {@link Optional#empty()}</li>
     * </ul>
     *
     * @param <Supplied> the type of object to be supplied
     * @param supplier the supplier to invoke (must not be {@code null})
     * @param contexts zero or more context objects that may be required by the supplier
     * @return the supplied object, or {@code null} if the supplier returns empty
     * @throws SupplyException if the supplier is null, no compatible context is found for a
     *                        contextual supplier, or an error occurs during supply
     */
    @SuppressWarnings("unchecked")
    public static <Supplied> Supplied contextualSupply(IObjectSupplier<Supplied> supplier, Object... contexts) throws SupplyException{

        if (supplier == null) {
            throw new SupplyException("Supplier cannot be null");
        }

        Supplied obj = null;

        if (supplier instanceof IContextualObjectSupplier<?, ?> contextualRaw) {
            IContextualObjectSupplier<Supplied, Object> contextual = (IContextualObjectSupplier<Supplied, Object>) contextualRaw;

            Object matchingContext = null;
            if (contexts != null) {
                for (Object ctx : contexts) {
                    if (ctx != null && contextual.getOwnerContextType().isAssignableFrom(ctx.getClass())) {
                        matchingContext = ctx;
                        break;
                    }
                }
            }

            if (matchingContext == null) {
                throw new SupplyException(
                        "No compatible context found for supplier expecting " + contextual.getOwnerContextType().getName());
            }

            obj = contextual.supply(matchingContext, contexts).orElse(null);
        } else {
            obj = supplier.supply().orElse(null);
        }

        return obj;
    }



}
