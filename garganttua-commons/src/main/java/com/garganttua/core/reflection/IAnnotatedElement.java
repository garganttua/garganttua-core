package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface IAnnotatedElement extends IReflectionUser {

    /**
     * Returns true if an annotation for the specified type
     * is <em>present</em> on this element, else false. This method
     * is designed primarily for convenient access to marker annotations.
     *
     * <p>
     * The truth value returned by this method is equivalent to:
     * {@code getAnnotation(annotationClass) != null}
     *
     * @implSpec The default implementation returns {@code
     * getAnnotation(annotationClass) != null}.
     *
     * @param annotationClass the IClass object corresponding to the
     *                        annotation type
     * @return true if an annotation for the specified annotation
     *         type is present on this element, else false
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    default boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>present</em>, else null.
     *
     * @param <T>             the type of the annotation to query for and return if
     *                        present
     * @param annotationClass the IClass object corresponding to the
     *                        annotation type
     * @return this element's annotation for the specified annotation type if
     *         present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    <T extends Annotation> T getAnnotation(IClass<T> annotationClass);

    /**
     * Returns annotations that are <em>present</em> on this element.
     *
     * If there are no annotations <em>present</em> on this element, the return
     * value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations present on this element
     * @since 1.5
     */
    Annotation[] getAnnotations();

    /**
     * Returns annotations that are <em>associated</em> with this element.
     *
     * If there are no annotations <em>associated</em> with this element, the return
     * value is an array of length 0.
     *
     * The difference between this method and {@link #getAnnotation(IClass)}
     * is that this method detects if its argument is a <em>repeatable
     * annotation type</em> (JLS {@jls 9.6}), and if so, attempts to find one or
     * more annotations of that type by "looking through" a container
     * annotation.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation first calls {@link
     *           #getDeclaredAnnotationsByType(IClass)} passing {@code
     * annotationClass} as the argument. If the returned array has
     *           length greater than zero, the array is returned. If the returned
     *           array is zero-length and this {@code AnnotatedElement} is a
     *           class and the argument type is an inheritable annotation type,
     *           and the superclass of this {@code AnnotatedElement} is non-null,
     *           then the returned result is the result of calling {@link
     *           #getAnnotationsByType(IClass)} on the superclass with {@code
     * annotationClass} as the argument. Otherwise, a zero-length
     *           array is returned.
     *
     * @param <T>             the type of the annotation to query for and return if
     *                        present
     * @param annotationClass the IClass object corresponding to the
     *                        annotation type
     * @return all this element's annotations for the specified annotation type if
     *         associated with this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T[] getAnnotationsByType(IClass<T> annotationClass) {
        /*
         * Definition of associated: directly or indirectly present OR
         * neither directly nor indirectly present AND the element is
         * a IClass, the annotation type is inheritable, and the
         * annotation type is associated with the superclass of the
         * element.
         */

        T[] result = getDeclaredAnnotationsByType(annotationClass);

        if (result.length == 0 && // Neither directly nor indirectly present
                this instanceof IClass && // the element is a class
                annotationClass.isAnnotationPresent(reflection().getClass(Inherited.class))) { // Inheritable
            IClass<?> superClass = ((IClass<?>) this).getSuperclass();
            if (superClass != null) {
                // Determine if the annotation is associated with the
                // superclass
                result = superClass.getAnnotationsByType(annotationClass);
            }
        }

        return result;
    }

    IReflection reflection();

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>directly present</em>, else null.
     *
     * This method ignores inherited annotations. (Returns null if no
     * annotations are directly present on this element.)
     *
     * @implSpec The default implementation first performs a null check
     *           and then loops over the results of {@link
     *           #getDeclaredAnnotations} returning the first annotation whose
     *           annotation type matches the argument type.
     *
     * @param <T>             the type of the annotation to query for and return if
     *                        directly present
     * @param annotationClass the IClass object corresponding to the
     *                        annotation type
     * @return this element's annotation for the specified annotation type if
     *         directly present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T getDeclaredAnnotation(IClass<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        // Loop over all directly-present annotations looking for a matching one
        for (Annotation annotation : getDeclaredAnnotations()) {
            if (annotationClass.getType().equals(annotation.annotationType())) {
                // More robust to do a dynamic cast at runtime instead
                // of compile-time only.
                return annotationClass.cast(annotation);
            }
        }
        return null;
    }

    /**
     * Returns this element's annotation(s) for the specified type if
     * such annotations are either <em>directly present</em> or
     * <em>indirectly present</em>. This method ignores inherited
     * annotations.
     *
     * If there are no specified annotations directly or indirectly
     * present on this element, the return value is an array of length
     * 0.
     *
     * The difference between this method and {@link
     * #getDeclaredAnnotation(IClass)} is that this method detects if its
     * argument is a <em>repeatable annotation type</em> (JLS {@jls 9.6}), and if
     * so,
     * attempts to find one or more annotations of that type by "looking
     * through" a container annotation if one is present.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation may call {@link
     *           #getDeclaredAnnotation(IClass)} one or more times to find a
     *           directly present annotation and, if the annotation type is
     *           repeatable, to find a container annotation. If annotations of
     *           the annotation type {@code annotationClass} are found to be both
     *           directly and indirectly present, then {@link
     *           #getDeclaredAnnotations()} will get called to determine the
     *           order of the elements in the returned array.
     *
     *           <p>
     *           Alternatively, the default implementation may call {@link
     *           #getDeclaredAnnotations()} a single time and the returned array
     *           examined for both directly and indirectly present
     *           annotations. The results of calling {@link
     *           #getDeclaredAnnotations()} are assumed to be consistent with the
     *           results of calling {@link #getDeclaredAnnotation(IClass)}.
     *
     * @param <T>             the type of the annotation to query for and return
     *                        if directly or indirectly present
     * @param annotationClass the IClass object corresponding to the
     *                        annotation type
     * @return all this element's annotations for the specified annotation type if
     *         directly or indirectly present on this element, else an array of
     *         length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    @SuppressWarnings("unchecked")
    default <T extends Annotation> T[] getDeclaredAnnotationsByType(IClass<T> annotationClass) {
        Objects.requireNonNull(annotationClass);

        List<T> result = new ArrayList<>();

        for (Annotation annotation : getDeclaredAnnotations()) {
            if (annotation.annotationType().equals(annotationClass.getClass())) {
                result.add(annotationClass.cast(annotation));
            }
        }

        Repeatable repeatable = annotationClass.getDeclaredAnnotation(reflection().getClass(Repeatable.class));

        if (repeatable != null) {
            Class<? extends Annotation> containerType = repeatable.value();

            for (Annotation annotation : getDeclaredAnnotations()) {
                if (annotation.annotationType().equals(containerType)) {
                    try {
                        IMethod valueMethod = reflection().getClass(containerType.getClass()).getMethod("value");
                        T[] values = (T[]) valueMethod.invoke(annotation);
                        Collections.addAll(result, values);
                    } catch (Exception e) {
                        throw new ReflectionException(e);
                    }
                }
            }
        }

        return result.toArray((T[]) Array.newInstance(annotationClass.getClass(), result.size()));
    }

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     *
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    Annotation[] getDeclaredAnnotations();
}
