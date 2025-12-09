package com.garganttua.core.injection;

import java.lang.reflect.AnnotatedElement;

/**
 * Functional interface for resolving injection elements based on annotations.
 *
 * <p>
 * An {@code IElementResolver} is responsible for analyzing annotated elements (fields, parameters, etc.)
 * and determining how to resolve their dependencies. Resolvers are registered with the DI context
 * and associated with specific annotation types, enabling custom injection behaviors beyond
 * standard type-based injection.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Custom resolver for @Property annotation
 * IElementResolver propertyResolver = (elementType, element) -> {
 *     Property annotation = element.getAnnotation(Property.class);
 *     String key = annotation.value();
 *     ISupplierBuilder<?, ?> supplier = ...;
 *     return new Resolved(true, elementType, supplier, false);
 * };
 *
 * // Register the resolver in the context
 * context.addResolver(Property.class, propertyResolver);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations should be stateless and thread-safe as they may be invoked concurrently.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IInjectableElementResolver
 * @see Resolved
 * @see AnnotatedElement
 */
@FunctionalInterface
public interface IElementResolver {

    /**
     * Resolves an annotated element to determine its dependency and how to provide it.
     *
     * <p>
     * This method analyzes the element's annotations and type to create a {@link Resolved}
     * result containing the supplier for the dependency. If the element cannot be resolved,
     * it should return a not-resolved result.
     * </p>
     *
     * @param elementType the type of the element to resolve
     * @param element the annotated element (field, parameter, etc.)
     * @return a {@link Resolved} instance containing the resolution result
     * @throws DiException if an error occurs during resolution
     */
    Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException;

}
