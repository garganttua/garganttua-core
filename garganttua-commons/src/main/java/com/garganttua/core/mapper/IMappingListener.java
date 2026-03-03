package com.garganttua.core.mapper;

import com.garganttua.core.reflection.IClass;

/**
 * Listener for mapping events. Invoked only at the top-level map() call,
 * not during recursive sub-mappings.
 *
 * @since 2.0.0-ALPHA01
 */
public interface IMappingListener {

	/**
	 * Called before a mapping operation starts.
	 *
	 * @param source the source object
	 * @param destClass the destination class
	 */
	void onBeforeMapping(Object source, IClass<?> destClass);

	/**
	 * Called after a mapping operation completes successfully.
	 *
	 * @param source the source object
	 * @param dest the mapped destination object
	 * @param durationNanos duration of the mapping in nanoseconds
	 */
	void onAfterMapping(Object source, Object dest, long durationNanos);

	/**
	 * Called when a mapping operation fails.
	 *
	 * @param source the source object
	 * @param destClass the destination class
	 * @param error the exception that occurred
	 */
	void onMappingError(Object source, IClass<?> destClass, Exception error);
}
