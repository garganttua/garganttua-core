package com.garganttua.core.reflection;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Interface mirroring {@link java.lang.reflect.Constructor}.
 *
 * <p>Runtime implementations wrap the actual {@code Constructor} object;
 * AOT implementations provide compile-time generated metadata and direct instantiation.</p>
 *
 * @param <T> the class in which the constructor is declared
 * @since 2.0.0-ALPHA01
 */
public interface IConstructor<T> extends IMember, GenericDeclaration {

	// --- Member (covariant override) ---

	@Override
	IClass<T> getDeclaringClass();

	// --- AccessibleObject ---

	void setAccessible(boolean flag);

	boolean trySetAccessible();

	boolean canAccess(Object obj);

	// --- GenericDeclaration ---

	TypeVariable<?>[] getTypeParameters();

	// --- Parameters ---

	IClass<?>[] getParameterTypes();

	Type[] getGenericParameterTypes();

	int getParameterCount();

	IParameter[] getParameters();

	// --- Exceptions ---

	IClass<?>[] getExceptionTypes();

	Type[] getGenericExceptionTypes();

	// --- Constructor properties ---

	boolean isVarArgs();

	String toGenericString();

	// --- Instantiation ---

	T newInstance(Object... initargs)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

	// --- Annotated types ---

	AnnotatedType getAnnotatedReturnType();

	AnnotatedType[] getAnnotatedParameterTypes();

	AnnotatedType[] getAnnotatedExceptionTypes();

	AnnotatedType getAnnotatedReceiverType();

	// --- AnnotatedElement ---

	boolean isAnnotationPresent(IClass<? extends Annotation> annotationClass);

	<A extends Annotation> A getAnnotation(IClass<A> annotationClass);

	Annotation[] getAnnotations();

	Annotation[] getDeclaredAnnotations();

	<A extends Annotation> A[] getAnnotationsByType(IClass<A> annotationClass);

	<A extends Annotation> A getDeclaredAnnotation(IClass<A> annotationClass);

	<A extends Annotation> A[] getDeclaredAnnotationsByType(IClass<A> annotationClass);
}
