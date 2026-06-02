package com.garganttua.core.reflection;

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
public interface IMethod extends IExecutable {

	// --- Return type ---

	IClass<?> getReturnType();

	Type getGenericReturnType();

	// --- Method-specific properties ---

	boolean isBridge();

	boolean isDefault();

	Object getDefaultValue();

	// --- Invocation ---

	/**
	 * Invokes this method. Mirror of
	 * {@link java.lang.reflect.Method#invoke(Object, Object...)}.
	 *
	 * @param obj  the instance to invoke on, or {@code null} for a static method
	 * @param args the method arguments
	 * @return the method result, or {@code null} for a {@code void} method
	 * @throws IllegalAccessException    if this method is inaccessible
	 * @throws IllegalArgumentException  if the arguments do not match the parameters
	 * @throws InvocationTargetException if the underlying method throws an exception
	 */
	Object invoke(Object obj, Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
