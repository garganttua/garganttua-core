package com.garganttua.core.lifecycle;

/**
 * Enumerates the states a component managed by {@link ILifecycle} can be in.
 *
 * <ul>
 *   <li>{@link #NEW} - instantiated but not yet initialized</li>
 *   <li>{@link #INITIALIZED} - {@code onInit()} completed</li>
 *   <li>{@link #STARTED} - {@code onStart()} completed and actively running</li>
 *   <li>{@link #FLUSHED} - buffers/caches cleared via {@code onFlush()}</li>
 *   <li>{@link #STOPPED} - gracefully shut down via {@code onStop()}</li>
 * </ul>
 *
 * @since 2.0.0-ALPHA01
 */
public enum LifecycleStatus {
    NEW, FLUSHED, STOPPED, INITIALIZED, STARTED
}
