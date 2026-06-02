package com.garganttua.core.reflection;

import java.lang.reflect.GenericSignatureFormatError;

/**
 * Mirror of {@link java.lang.reflect.GenericDeclaration}: an element (class,
 * method, or constructor) that may declare type variables.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IGenericDeclaration extends IAnnotatedElement {
    /**
     * Returns an array of {@code TypeVariable} objects that
     * represent the type variables declared by the generic
     * declaration represented by this {@code GenericDeclaration}
     * object, in declaration order.  Returns an array of length 0 if
     * the underlying generic declaration declares no type variables.
     *
     * @return an array of {@code TypeVariable} objects that represent
     *     the type variables declared by this generic declaration
     * @throws GenericSignatureFormatError if the generic
     *     signature of this generic declaration does not conform to
     *     the format specified in
     *     <cite>The Java Virtual Machine Specification</cite>
     */
    public ITypeVariable<?>[] getTypeParameters();
}
