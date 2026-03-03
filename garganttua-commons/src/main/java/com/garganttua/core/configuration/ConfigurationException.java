package com.garganttua.core.configuration;

import com.garganttua.core.CoreException;

public class ConfigurationException extends CoreException {

    private static final long serialVersionUID = 1L;

    public static final int CONFIGURATION_ERROR = 15;

    public ConfigurationException(String message) {
        super(CONFIGURATION_ERROR, message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(CONFIGURATION_ERROR, message, cause);
    }

    public ConfigurationException(Throwable cause) {
        super(CONFIGURATION_ERROR, cause);
    }
}
