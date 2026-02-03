package com.garganttua.core.mutex.context;

import com.garganttua.core.mutex.IMutexManager;

/**
 * Thread-local context holder for the current IMutexManager.
 *
 * <p>
 * This class provides access to the mutex manager from expression functions
 * and other contexts where dependency injection is not directly available.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Set the manager (typically in initialization code)
 * MutexContext.set(mutexManager);
 *
 * // Get the manager in expression functions
 * IMutexManager manager = MutexContext.get();
 *
 * // Clear when done
 * MutexContext.clear();
 * }</pre>
 *
 * @since 2.0.0-ALPHA01
 */
public final class MutexContext {

    private static final ThreadLocal<IMutexManager> CONTEXT = new ThreadLocal<>();

    private MutexContext() {
        // Utility class
    }

    /**
     * Sets the mutex manager for the current thread.
     *
     * @param manager the mutex manager to set
     */
    public static void set(IMutexManager manager) {
        CONTEXT.set(manager);
    }

    /**
     * Gets the mutex manager for the current thread.
     *
     * @return the current mutex manager, or null if not set
     */
    public static IMutexManager get() {
        return CONTEXT.get();
    }

    /**
     * Clears the mutex manager for the current thread.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * Checks if a mutex manager is available in the current context.
     *
     * @return true if a mutex manager is set, false otherwise
     */
    public static boolean isAvailable() {
        return CONTEXT.get() != null;
    }
}
