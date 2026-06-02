package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.reflect.Executable}.
 *
 * <p>Provides the common contract shared by both {@link IMethod} and {@link IConstructor},
 * covering parameter introspection, exception types, annotated types, and accessibility.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IExecutable extends IMember, IGenericDeclaration {

	// --- AccessibleObject ---

	/**
	 * Sets the accessibility flag for this executable.
	 *
	 * @param flag {@code true} to suppress access checks
	 */
	void setAccessible(boolean flag);

	/**
	 * Attempts to make this executable accessible, returning the outcome.
	 *
	 * @return {@code true} if accessibility was successfully enabled
	 */
	boolean trySetAccessible();

	/**
	 * Tests whether the caller can access this executable for the given object.
	 *
	 * @param obj the instance to test against, or {@code null} for static members
	 * @return {@code true} if access is permitted
	 */
	boolean canAccess(Object obj);

	// --- Parameters ---

	/**
	 * @return the declared parameter types, in declaration order
	 */
	IClass<?>[] getParameterTypes();

	/**
	 * @return the generic parameter types, in declaration order
	 */
	Type[] getGenericParameterTypes();

	/**
	 * @return the number of formal parameters
	 */
	int getParameterCount();

	/**
	 * @return the formal parameters, in declaration order
	 */
	IParameter[] getParameters();

	/**
	 * @return the annotations on each parameter, indexed by parameter position
	 */
	Annotation[][] getParameterAnnotations();

	// --- Exceptions ---

	/**
	 * @return the declared checked exception types
	 */
	IClass<?>[] getExceptionTypes();

	/**
	 * @return the generic declared exception types
	 */
	Type[] getGenericExceptionTypes();

	// --- Executable properties ---

	/**
	 * @return {@code true} if this executable was declared with a variable number of arguments
	 */
	boolean isVarArgs();

	/**
	 * @return a string describing this executable including generic type information
	 */
	String toGenericString();

	// --- Annotated types ---

	/**
	 * @return the annotated return type, or the annotated declaring type for a constructor
	 */
	AnnotatedType getAnnotatedReturnType();

	/**
	 * @return the annotated parameter types, in declaration order
	 */
	AnnotatedType[] getAnnotatedParameterTypes();

	/**
	 * @return the annotated declared exception types
	 */
	AnnotatedType[] getAnnotatedExceptionTypes();

	/**
	 * @return the annotated receiver type, or {@code null} when none is present
	 */
	AnnotatedType getAnnotatedReceiverType();

}
