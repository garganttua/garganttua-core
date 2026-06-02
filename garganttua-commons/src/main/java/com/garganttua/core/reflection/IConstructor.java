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

	/**
	 * Creates a new instance using this constructor. Mirror of
	 * {@link java.lang.reflect.Constructor#newInstance(Object...)}.
	 *
	 * @param initargs the constructor arguments
	 * @return the newly created instance
	 * @throws InstantiationException    if the declaring class is abstract
	 * @throws IllegalAccessException    if this constructor is inaccessible
	 * @throws IllegalArgumentException  if the arguments do not match the parameters
	 * @throws InvocationTargetException if the constructor throws an exception
	 */
	T newInstance(Object... initargs)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException;

}
