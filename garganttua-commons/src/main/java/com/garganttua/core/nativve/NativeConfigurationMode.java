package com.garganttua.core.nativve;

/**
 * Enumeration defining how native image configuration should be merged or replaced.
 *
 * <p>
 * This enum controls the behavior when multiple native image configurations are present.
 * Currently, only override mode is fully supported.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public enum NativeConfigurationMode {

    /**
     * Override mode - existing configuration is replaced entirely.
     * <p>
     * This is the only mode currently supported.
     * </p>
     */
    override,

    /**
     * Merge mode - new configuration is merged with existing configuration.
     * <p>
     * Note: This mode is not yet fully implemented.
     * </p>
     */
    merge

}
