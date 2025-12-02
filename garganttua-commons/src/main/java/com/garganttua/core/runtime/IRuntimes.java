package com.garganttua.core.runtime;

/**
 * Marker interface for runtime collections.
 *
 * <p>
 * IRuntimes serves as a type marker for collections that contain multiple runtime instances.
 * It is primarily used for type-safety when working with runtime registries or collections
 * in the dependency injection context.
 * </p>
 *
 * <p>
 * Implementations typically provide access to multiple {@link IRuntime} instances by name
 * or other identifiers, allowing for centralized runtime management and lookup.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see IRuntime
 * @see com.garganttua.core.runtime.dsl.IRuntimesBuilder
 */
public interface IRuntimes {

}
