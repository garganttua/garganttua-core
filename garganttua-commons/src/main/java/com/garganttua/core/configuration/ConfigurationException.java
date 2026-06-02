package com.garganttua.core.configuration;

import com.garganttua.core.CoreException;

/**
 * Exception raised while loading, parsing, or applying configuration.
 *
 * <p>
 * Thrown when a configuration source cannot be read, a format cannot parse its
 * input, or a parsed node cannot be mapped onto a target builder. Carries the
 * {@link #CONFIGURATION_ERROR} code.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 * @see CoreException
 */
public class ConfigurationException extends CoreException {

    private static final long serialVersionUID = 1L;

    /** Error code reported by all configuration failures. */
    public static final int CONFIGURATION_ERROR = 15;

    /**
     * Constructs a new exception with the given detail message.
     *
     * @param message description of the configuration failure
     */
    public ConfigurationException(String message) {
        super(CONFIGURATION_ERROR, message);
    }

    /**
     * Constructs a new exception with the given detail message and cause.
     *
     * @param message description of the configuration failure
     * @param cause   the underlying cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(CONFIGURATION_ERROR, message, cause);
    }

    /**
     * Constructs a new exception wrapping the given cause.
     *
     * @param cause the underlying cause
     */
    public ConfigurationException(Throwable cause) {
        super(CONFIGURATION_ERROR, cause);
    }
}
