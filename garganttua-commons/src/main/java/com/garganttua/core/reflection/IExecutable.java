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

	void setAccessible(boolean flag);

	boolean trySetAccessible();

	boolean canAccess(Object obj);

	// --- Parameters ---

	IClass<?>[] getParameterTypes();

	Type[] getGenericParameterTypes();

	int getParameterCount();

	IParameter[] getParameters();

	Annotation[][] getParameterAnnotations();

	// --- Exceptions ---

	IClass<?>[] getExceptionTypes();

	Type[] getGenericExceptionTypes();

	// --- Executable properties ---

	boolean isVarArgs();

	String toGenericString();

	// --- Annotated types ---

	AnnotatedType getAnnotatedReturnType();

	AnnotatedType[] getAnnotatedParameterTypes();

	AnnotatedType[] getAnnotatedExceptionTypes();

	AnnotatedType getAnnotatedReceiverType();

}
