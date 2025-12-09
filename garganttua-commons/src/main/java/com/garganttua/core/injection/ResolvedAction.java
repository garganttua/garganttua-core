package com.garganttua.core.injection;

import com.garganttua.core.supply.ISupplier;
import com.garganttua.core.supply.dsl.ISupplierBuilder;

/**
 * Functional interface for handling successfully resolved dependencies.
 *
 * <p>
 * {@code ResolvedAction} defines a callback action that is invoked when a dependency resolution
 * succeeds. It receives the supplier builder for the resolved element and a flag indicating whether
 * the element is nullable. This interface is used in conjunction with {@link Resolved} to provide
 * conditional execution patterns for dependency resolution results.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Use with Resolved.ifResolved
 * Resolved resolved = ...;
 * resolved.ifResolved((supplier, nullable) -> {
 *     if (nullable) {
 *         // Handle nullable dependency
 *         System.out.println("Optional dependency resolved");
 *     } else {
 *         // Handle required dependency
 *         System.out.println("Required dependency resolved");
 *     }
 * });
 *
 * // Or use with ifResolvedOrElse
 * resolved.ifResolvedOrElse(
 *     (supplier, nullable) -> {
 *         // Dependency was resolved successfully
 *         buildDependency(supplier);
 *     },
 *     (nullable) -> {
 *         // Dependency was not resolved
 *         handleMissingDependency(nullable);
 *     }
 * );
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 * @see Resolved
 * @see NotResolvedAction
 * @see ISupplierBuilder
 */
@FunctionalInterface
public interface ResolvedAction {

    /**
     * Handles a successfully resolved dependency.
     *
     * <p>
     * This method is invoked when a dependency resolution succeeds. The element supplier
     * provides the means to obtain the dependency instance, and the nullable flag indicates
     * whether null values are acceptable for this dependency.
     * </p>
     *
     * @param elementSupplier the supplier builder for obtaining the resolved element
     * @param nullable {@code true} if the element accepts null values, {@code false} if required
     */
    void ifResolved(ISupplierBuilder<?, ? extends ISupplier<?>> elementSupplier, boolean nullable);

}
