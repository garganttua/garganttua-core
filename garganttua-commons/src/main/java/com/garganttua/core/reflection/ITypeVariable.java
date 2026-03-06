package com.garganttua.core.reflection;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.MalformedParameterizedTypeException;
import java.lang.reflect.Type;

public interface ITypeVariable<D extends IGenericDeclaration> extends Type, IAnnotatedElement {
    /**
     * Returns an array of {@code Type} objects representing the
     * upper bound(s) of this type variable. If no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * <p>
     * For each upper bound B:
     * <ul>
     * <li>if B is a parameterized
     * type or a type variable, it is created, (see {@link
     * java.lang.reflect.ParameterizedType ParameterizedType} for the
     * details of the creation process for parameterized types).
     * <li>Otherwise, B is resolved.
     * </ul>
     *
     * @throws TypeNotPresentException             if any of the
     *                                             bounds refers to a non-existent
     *                                             type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *                                             bounds refer to a parameterized
     *                                             type that cannot be instantiated
     *                                             for any reason
     * @return an array of {@code Type}s representing the upper
     *         bound(s) of this type variable
     */
    Type[] getBounds();

    /**
     * Returns the {@code GenericDeclaration} object representing the
     * generic declaration declared for this type variable.
     *
     * @return the generic declaration declared for this type variable.
     *
     * @since 1.5
     */
    D getGenericDeclaration();

    /**
     * Returns the name of this type variable, as it occurs in the source code.
     *
     * @return the name of this type variable, as it appears in the source code
     */
    String getName();

    /**
     * Returns an array of AnnotatedType objects that represent the use of
     * types to denote the upper bounds of the type parameter represented by
     * this TypeVariable. The order of the objects in the array corresponds to
     * the order of the bounds in the declaration of the type parameter. Note that
     * if no upper bound is explicitly declared, the upper bound is unannotated
     * {@code Object}.
     *
     * @return an array of objects representing the upper bound(s) of the type
     *         variable
     * @since 1.8
     */
    AnnotatedType[] getAnnotatedBounds();
}