package com.garganttua.core.reflection.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Provides access to compile-time generated annotation indices.
 *
 * <p>
 * This interface provides fast lookup of classes and methods annotated with
 * {@link Indexed @Indexed} annotations. Unlike runtime classpath scanning,
 * this uses pre-computed index files generated at compile-time, resulting
 * in near-instantaneous lookups regardless of classpath size.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IAnnotationIndex index = new AnnotationIndex();
 *
 * // Find all classes annotated with @Singleton
 * List<Class<?>> singletons = index.getClassesWithAnnotation(Singleton.class);
 *
 * // Find all methods annotated with @Expression
 * List<Method> expressions = index.getMethodsWithAnnotation(Expression.class);
 * }</pre>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li>Lookup time: O(n) where n is the number of indexed elements</li>
 *   <li>No classpath scanning required</li>
 *   <li>Index loading is lazy and cached</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 * @see Indexed
 */
public interface IAnnotationIndex {

    /**
     * Retrieves all classes annotated with the specified annotation.
     *
     * <p>
     * This method returns classes from the compile-time generated index.
     * Only annotations marked with {@link Indexed @Indexed} will have
     * index data available.
     * </p>
     *
     * @param annotation the annotation type to search for
     * @return a list of annotated classes (never {@code null}, may be empty)
     */
    List<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotation);

    /**
     * Retrieves all methods annotated with the specified annotation.
     *
     * <p>
     * This method returns methods from the compile-time generated index.
     * Only annotations marked with {@link Indexed @Indexed} will have
     * index data available.
     * </p>
     *
     * @param annotation the annotation type to search for
     * @return a list of annotated methods (never {@code null}, may be empty)
     */
    List<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotation);

    /**
     * Retrieves all classes annotated with the specified annotation,
     * filtered by package prefix.
     *
     * @param annotation the annotation type to search for
     * @param packagePrefix the package prefix to filter by (e.g., "com.example")
     * @return a list of annotated classes in the specified package (never {@code null})
     */
    List<Class<?>> getClassesWithAnnotation(Class<? extends Annotation> annotation, String packagePrefix);

    /**
     * Retrieves all methods annotated with the specified annotation,
     * filtered by package prefix.
     *
     * @param annotation the annotation type to search for
     * @param packagePrefix the package prefix to filter by (e.g., "com.example")
     * @return a list of annotated methods in the specified package (never {@code null})
     */
    List<Method> getMethodsWithAnnotation(Class<? extends Annotation> annotation, String packagePrefix);

    /**
     * Checks if an index exists for the specified annotation.
     *
     * @param annotation the annotation type to check
     * @return {@code true} if an index exists, {@code false} otherwise
     */
    boolean hasIndex(Class<? extends Annotation> annotation);
}
