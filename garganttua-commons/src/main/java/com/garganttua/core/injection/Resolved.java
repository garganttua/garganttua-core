package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;

import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.dsl.IObjectSupplierBuilder;

/**
 * Represents the result of resolving an injectable element in the dependency injection system.
 *
 * <p>
 * {@code Resolved} encapsulates the outcome of attempting to resolve a dependency for an
 * annotated element (field, parameter, etc.). It contains information about whether the
 * resolution was successful, the element's type, a supplier builder for obtaining the
 * dependency, and whether the element is nullable. This record is used throughout the
 * resolution process to track and propagate dependency resolution results.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a resolved result
 * IObjectSupplierBuilder<?, ?> supplierBuilder = ...;
 * Resolved resolved = new Resolved(true, MyService.class, supplierBuilder, false);
 *
 * // Use conditional actions
 * resolved.ifResolved((supplier, nullable) -> {
 *     // Handle resolved dependency
 * });
 *
 * // Handle both cases
 * resolved.ifResolvedOrElse(
 *     (supplier, nullable) -> {
 *         // Handle resolved
 *     },
 *     (nullable) -> {
 *         // Handle not resolved
 *     }
 * );
 * }</pre>
 *
 * @param resolved whether the element was successfully resolved
 * @param elementType the type of the element
 * @param elementSupplier the supplier builder for the resolved element (null if not resolved)
 * @param nullable whether the element accepts null values
 * @since 2.0.0-ALPHA01
 * @see IElementResolver
 * @see IInjectableElementResolver
 * @see ResolvedAction
 * @see NotResolvedAction
 */
public record Resolved(boolean resolved, Class<?> elementType, IObjectSupplierBuilder<?, ? extends IObjectSupplier<?>> elementSupplier, boolean nullable) {

    /**
     * Creates a not-resolved result for an element.
     *
     * <p>
     * This factory method is used when an element cannot be resolved. It preserves
     * the element's type and nullable status while indicating resolution failure.
     * </p>
     *
     * @param elementType the type of the element that could not be resolved
     * @param annotatedElement the annotated element to check for nullable annotations
     * @return a {@code Resolved} instance indicating resolution failure
     */
    public static Resolved notResolved(Class<?> elementType, AnnotatedElement annotatedElement){
        return new Resolved(false, elementType, null, IInjectableElementResolver.isNullable(annotatedElement));
    }

    /**
     * Executes an action if the element was resolved.
     *
     * <p>
     * This method provides a conditional execution pattern similar to {@code Optional.ifPresent()}.
     * The action is only executed if the resolution was successful.
     * </p>
     *
     * @param action the action to execute if resolved
     */
    public void ifResolved(ResolvedAction action){
        if( resolved )
            action.ifResolved(elementSupplier, nullable);
    }

    /**
     * Executes one of two actions depending on resolution status.
     *
     * <p>
     * This method provides a conditional execution pattern similar to {@code Optional.ifPresentOrElse()}.
     * The first action is executed if resolved, otherwise the second action is executed.
     * </p>
     *
     * @param action the action to execute if resolved
     * @param naction the action to execute if not resolved
     */
    public void ifResolvedOrElse(ResolvedAction action, NotResolvedAction naction){
        if( resolved )
            action.ifResolved(elementSupplier, nullable);
        else
            naction.ifNotResolved(nullable);
    }

}
