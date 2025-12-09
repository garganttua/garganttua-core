package com.garganttua.core.supply;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class providing helper methods for working with object suppliers.
 *
 * <p>
 * {@code Supplier} offers static utility methods that simplify supplier usage,
 * particularly for contextual suppliers. The primary use case is automatically
 * matching and providing the correct context to {@link IContextualSupplier}
 * instances, eliminating the need for manual context type checking and casting.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Define suppliers
 * ISupplier<Database> dbSupplier = ...;  // Contextual supplier needing DiContext
 * ISupplier<Logger> loggerSupplier = ...; // Simple supplier, no context needed
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
 * For contextual suppliers, the {@link #contextualSupply(ISupplier, Object...)}
 * method automatically searches the provided contexts array for a compatible context
 * type using {@link Class#isAssignableFrom(Class)}. This supports both exact type
 * matches and subtype relationships.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see ISupplier
 * @see IContextualSupplier
 */
@Slf4j
public class Supplier {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Supplier() {
    }

    /**
     * Supplies an object from the given supplier, automatically matching contexts if needed.
     *
     * <p>
     * This method provides a unified interface for working with both contextual and
     * non-contextual suppliers. For {@link IContextualSupplier} instances, it
     * automatically searches the provided contexts array for a compatible context type
     * and delegates to {@link IContextualSupplier#supply(Object, Object...)}.
     * For standard {@link ISupplier} instances, it simply calls {@link ISupplier#supply()}.
     * </p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>If supplier is contextual: Searches for matching context, throws exception if not found</li>
     *   <li>If supplier is non-contextual: Calls {@code supply()} directly, ignoring contexts</li>
     *   <li>Returns {@code null} if the supplier returns an empty Optional</li>
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
    public static <Supplied> Supplied contextualSupply(ISupplier<Supplied> supplier, Object... contexts) throws SupplyException{
        log.atTrace().log("Entering contextualSupply with supplier={}, contexts count={}", supplier, contexts != null ? contexts.length : 0);

        if (supplier == null) {
            log.atError().log("Supplier cannot be null");
            throw new SupplyException("Supplier cannot be null");
        }

        Supplied obj = null;

        if (supplier instanceof IContextualSupplier<?, ?> contextualRaw) {
            log.atDebug().log("Supplier is contextual, searching for matching context");
            IContextualSupplier<Supplied, Object> contextual = (IContextualSupplier<Supplied, Object>) contextualRaw;

            Object matchingContext = null;
            if (contexts != null && !Void.class.isAssignableFrom(contextual.getOwnerContextType())) {
                for (Object ctx : contexts) {
                    if (ctx != null && contextual.getOwnerContextType().isAssignableFrom(ctx.getClass())) {
                        matchingContext = ctx;
                        log.atDebug().log("Found matching context of type {}", ctx.getClass().getName());
                        break;
                    }
                }
            }

            if (matchingContext == null && !Void.class.isAssignableFrom(contextual.getOwnerContextType())) {
                log.atError().log("No compatible context found for supplier expecting {}", contextual.getOwnerContextType().getName());
                throw new SupplyException(
                        "No compatible context found for supplier expecting " + contextual.getOwnerContextType().getName());
            }

            obj = contextual.supply(matchingContext, contexts).orElse(null);
            log.atInfo().log("Contextual supply completed, result is {}", obj != null ? "present" : "null");
        } else {
            log.atDebug().log("Supplier is non-contextual, calling supply() directly");
            obj = supplier.supply().orElse(null);
            log.atInfo().log("Non-contextual supply completed, result is {}", obj != null ? "present" : "null");
        }

        log.atTrace().log("Exiting contextualSupply");
        return obj;
    }

    /**
     * Supplies an object from the given supplier, recursively resolving nested suppliers.
     *
     * <p>
     * This method extends {@link #contextualSupply(ISupplier, Object...)} to handle cases
     * where a supplier returns another supplier. It recursively resolves the chain of
     * suppliers until a non-supplier value is obtained or {@code null} is returned.
     * </p>
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *   <li>If the result is an {@link ISupplier}, recursively calls this method</li>
     *   <li>If the result is not a supplier, returns it directly</li>
     *   <li>Returns {@code null} if any supplier in the chain returns {@code null}</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * // Supplier that returns another supplier
     * ISupplier<ISupplier<String>> nestedSupplier = ...;
     *
     * // Automatically resolves the nested structure
     * String result = Supplier.contextualRecursiveSupply(nestedSupplier, contexts);
     * }</pre>
     *
     * @param <Supplied> the type of object to be supplied
     * @param supplier the supplier to invoke (must not be {@code null})
     * @param contexts zero or more context objects that may be required by suppliers
     * @return the supplied object after recursive resolution, or {@code null} if any supplier returns null
     * @throws SupplyException if the supplier is null, no compatible context is found for a
     *                        contextual supplier, or an error occurs during supply
     */
    public static Object contextualRecursiveSupply(ISupplier<?> supplier, Object... contexts) throws SupplyException {
        log.atTrace().log("Entering contextualRecursiveSupply with supplier={}, contexts count={}", supplier, contexts != null ? contexts.length : 0);

        Object result = contextualSupply(supplier, contexts);

        if (result == null) {
            log.atDebug().log("Supplier returned null, ending recursion");
            return null;
        }

        if (result instanceof ISupplier<?>) {
            log.atDebug().log("Result is a supplier, recursively resolving");
            return contextualRecursiveSupply((ISupplier<?>) result, contexts);
        }

        log.atTrace().log("Exiting contextualRecursiveSupply with non-supplier result");
        return result;
    }


}
