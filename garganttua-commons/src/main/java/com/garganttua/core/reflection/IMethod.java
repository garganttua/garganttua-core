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

	Object invoke(Object obj, Object... args)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
