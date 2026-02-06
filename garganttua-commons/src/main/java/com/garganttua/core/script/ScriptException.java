package com.garganttua.core.script;

import com.garganttua.core.CoreException;

/**
 * Exception for script execution errors.
 *
 * <p>
 * ScriptException is thrown when errors occur during script parsing,
 * compilation, or execution. It extends {@link CoreException} with
 * the {@link CoreException#SCRIPT_ERROR} error code.
 * </p>
 *
 * @since 2.0.0-ALPHA01
 */
public class ScriptException extends CoreException {

    private static final long serialVersionUID = 1L;

    public ScriptException(String message) {
        super(SCRIPT_ERROR, message);
    }

    public ScriptException(String message, Throwable cause) {
        super(SCRIPT_ERROR, message, cause);
    }

    public ScriptException(Throwable cause) {
        super(SCRIPT_ERROR, cause);
    }
}
