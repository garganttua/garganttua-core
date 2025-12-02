package com.garganttua.core.injection;

/**
 * Functional interface for handling unresolved dependencies.
 *
 * <p>
 * {@code NotResolvedAction} defines a callback action that is invoked when a dependency resolution
 * fails. It receives a flag indicating whether the element is nullable, which helps determine whether
 * the resolution failure should be treated as an error or as an acceptable state. This interface is
 * used in conjunction with {@link Resolved} to provide conditional execution patterns for dependency
 * resolution results.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use with Resolved.ifResolvedOrElse
 * Resolved resolved = ...;
 * resolved.ifResolvedOrElse(
 *     (supplier, nullable) -> {
 *         // Dependency was resolved successfully
 *         System.out.println("Dependency found and resolved");
 *     },
 *     (nullable) -> {
 *         // Dependency was not resolved
 *         if (nullable) {
 *             System.out.println("Optional dependency not found, using null");
 *         } else {
 *             throw new DiException("Required dependency not found");
 *         }
 *     }
 * );
 *
 * // Custom handler for missing dependencies
 * NotResolvedAction handler = (nullable) -> {
 *     if (nullable) {
 *         logger.debug("Optional dependency not resolved");
 *     } else {
 *         logger.error("Required dependency not resolved");
 *         throw new DiException("Missing required dependency");
 *     }
 * };
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Resolved
 * @see ResolvedAction
 */
@FunctionalInterface
public interface NotResolvedAction {

    /**
     * Handles an unresolved dependency.
     *
     * <p>
     * This method is invoked when a dependency resolution fails. The nullable flag indicates
     * whether the missing dependency is optional (nullable) or required. For nullable elements,
     * the resolution failure may be acceptable and null can be injected. For non-nullable elements,
     * the resolution failure should typically result in an exception.
     * </p>
     *
     * @param nullable {@code true} if the element accepts null values (optional dependency),
     *                 {@code false} if the element is required
     */
    void ifNotResolved(boolean nullable);

}
