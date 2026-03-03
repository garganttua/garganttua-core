package com.garganttua.core.reflection;

import java.lang.reflect.AnnotatedType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 * Interface mirroring {@link java.lang.reflect.Method}.
 *
 * <p>
 * Runtime implementations wrap the actual {@code Method} object;
 * AOT implementations provide compile-time generated metadata and direct
 * invocation.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMethod extends IMember, IGenericDeclaration {

	// --- AccessibleObject ---

	void setAccessible(boolean flag);

	boolean trySetAccessible();

	boolean canAccess(Object obj);

	// --- GenericDeclaration ---

	ITypeVariable<?>[] getTypeParameters();

	// --- Return type ---

	IClass<?> getReturnType();

	Type getGenericReturnType();

	// --- Parameters ---

	IClass<?>[] getParameterTypes();

	Type[] getGenericParameterTypes();

	int getParameterCount();

	IParameter[] getParameters();

	// --- Exceptions ---

	IClass<?>[] getExceptionTypes();

	Type[] getGenericExceptionTypes();

	// --- Method properties ---

	boolean isVarArgs();

	boolean isBridge();

	boolean isDefault();

	Object getDefaultValue();

	String toGenericString();

	// --- Invocation ---

	Object invoke(Object obj, Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

	// --- Annotated types ---

	AnnotatedType getAnnotatedReturnType();

	AnnotatedType[] getAnnotatedParameterTypes();

	AnnotatedType[] getAnnotatedExceptionTypes();

	AnnotatedType getAnnotatedReceiverType();

}
