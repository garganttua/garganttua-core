package com.garganttua.core.reflection;

/**
 * Interface for components that need access to an {@link IReflection} instance.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IReflectionUser {

	/**
	 * Returns the reflection facade used by this component.
	 *
	 * @return the {@link IReflection} instance
	 */
	IReflection reflection();
}
