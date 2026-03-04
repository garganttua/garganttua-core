package com.garganttua.core.reflection;

import java.lang.reflect.InvocationTargetException;

/**
 * Interface mirroring {@link java.lang.reflect.Constructor}.
 *
 * <p>Runtime implementations wrap the actual {@code Constructor} object;
 * AOT implementations provide compile-time generated metadata and direct instantiation.</p>
 *
 * @param <T> the class in which the constructor is declared
 * @since 2.0.0-ALPHA01
 */
public interface IConstructor<T> extends IExecutable {

	// --- Instantiation ---

	T newInstance(Object... initargs)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
