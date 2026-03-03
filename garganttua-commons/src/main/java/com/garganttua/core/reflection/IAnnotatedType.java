package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;

public interface IAnnotatedType extends IAnnotatedElement {
   /**
     * Returns the potentially annotated type that this type is a member of, if
     * this type represents a nested type. For example, if this type is
     * {@code @TA O<T>.I<S>}, return a representation of {@code @TA O<T>}.
     *
     * <p>Returns {@code null} if this {@code AnnotatedType} represents a
     *     top-level class or interface, or a local or anonymous class, or
     *     a primitive type, or void.
     *
     * <p>Returns {@code null} if this {@code AnnotatedType} is an instance of
     *     {@code AnnotatedArrayType}, {@code AnnotatedTypeVariable}, or
     *     {@code AnnotatedWildcardType}.
     *
     * @implSpec
     * This default implementation returns {@code null} and performs no other
     * action.
     *
     * @return an {@code AnnotatedType} object representing the potentially
     *     annotated type that this type is a member of, or {@code null}
     * @throws TypeNotPresentException if the owner type
     *     refers to a non-existent class or interface declaration
     * @throws MalformedParameterizedTypeException if the owner type
     *     refers to a parameterized type that cannot be instantiated
     *     for any reason
     *
     * @since 9
     */
    default AnnotatedType getAnnotatedOwnerType() {
        return null;
    }

    /**
     * Returns the underlying type that this annotated type represents.
     *
     * @return the type this annotated type represents
     */
    public Type getType();

    /**
     * {@inheritDoc}
     * <p>Note that any annotation returned by this method is a type
     * annotation.
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    <T extends Annotation> T getAnnotation(IClass<T> annotationClass);

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are type
     * annotations.
     */
    @Override
    Annotation[] getAnnotations();

    /**
     * {@inheritDoc}
     * <p>Note that any annotations returned by this method are type
     * annotations.
     */
    @Override
    Annotation[] getDeclaredAnnotations();
}
