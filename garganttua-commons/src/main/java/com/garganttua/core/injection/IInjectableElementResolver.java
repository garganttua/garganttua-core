package com.garganttua.core.injection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Executable;
import java.util.Set;

import jakarta.annotation.Nullable;
import lombok.NonNull;

/**
 * Manages resolution of injectable elements such as constructor parameters, method parameters, and fields.
 *
 * <p>
 * {@code IInjectableElementResolver} serves as the central registry for element resolvers,
 * coordinating dependency resolution across the DI system. It maintains a mapping between
 * annotation types and their corresponding resolvers, enabling extensible and customizable
 * injection behaviors. This interface is essential for automatic dependency injection.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Add custom resolvers for specific annotations
 * IInjectableElementResolver resolver = ...;
 *
 * // Register a resolver for @Property annotations
 * resolver.addResolver(Property.class, (type, element) -> {
 *     Property prop = element.getAnnotation(Property.class);
 *     return resolveProperty(prop.value(), type);
 * });
 *
 * // Resolve a constructor's parameters
 * Constructor<?> constructor = MyClass.class.getConstructor(String.class, int.class);
 * Set<Resolved> parameters = resolver.resolve(constructor);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations should be thread-safe for read operations but may require synchronization
 * when adding new resolvers.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IElementResolver
 * @see Resolved
 * @see IInjectionContext
 */
public interface IInjectableElementResolver {

    /**
     * Resolves a single annotated element to determine its dependency.
     *
     * <p>
     * This method examines the element's annotations, applies registered resolvers,
     * and returns a {@link Resolved} result indicating how to provide the dependency.
     * </p>
     *
     * @param elementType the type of the element to resolve
     * @param element the annotated element (field, parameter, etc.)
     * @return a {@link Resolved} instance containing the resolution result
     * @throws DiException if an error occurs during resolution
     */
    Resolved resolve(Class<?> elementType, AnnotatedElement element) throws DiException;

    /**
     * Resolves all parameters of an executable (constructor or method).
     *
     * <p>
     * This method processes all parameters of the executable, resolving each one
     * and returning a set of {@link Resolved} results in the same order as the parameters.
     * </p>
     *
     * @param method the executable whose parameters should be resolved
     * @return a set of {@link Resolved} instances for each parameter
     * @throws DiException if an error occurs during resolution
     */
    Set<Resolved> resolve(Executable method) throws DiException;

    /**
     * Registers a custom resolver for a specific annotation type.
     *
     * <p>
     * When an element annotated with the specified annotation is encountered,
     * the provided resolver will be invoked to determine how to inject it.
     * </p>
     *
     * @param annotation the annotation class to associate with the resolver
     * @param resolver the resolver to handle elements with this annotation
     */
    void addResolver(Class<? extends Annotation> annotation, IElementResolver resolver);

    /**
     * Determines if an annotated element is nullable.
     *
     * <p>
     * An element is considered nullable if it has a {@code @Nullable} annotation
     * and is not nullable if it has a {@code @NonNull} annotation. If neither
     * annotation is present, the element is considered not nullable by default.
     * </p>
     *
     * @param annotatedElement the element to check
     * @return {@code true} if the element is nullable, {@code false} otherwise
     */
    public static boolean isNullable(AnnotatedElement annotatedElement) {
        if (annotatedElement.getAnnotation(Nullable.class) != null)
            return true;
        if (annotatedElement.getAnnotation(NonNull.class) != null)
            return false;
        return false;
    }

}
