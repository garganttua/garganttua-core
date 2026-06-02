package com.garganttua.core.expression;

import com.garganttua.core.CoreException;

/**
 * Thrown when parsing or evaluating an expression fails.
 *
 * <p>Carries the {@link CoreException#EXPRESSION_ERROR} error code.</p>
 *
 * @since 2.0.0-ALPHA01
 */
public class ExpressionException extends CoreException {

    /**
     * Creates an exception with a message and underlying cause.
     *
     * @param message   the detail message
     * @param exception the underlying cause
     */
    protected ExpressionException(String message, Throwable exception) {
        super(CoreException.EXPRESSION_ERROR, message, exception);
    }

    /**
     * Creates an exception with a detail message.
     *
     * @param string the detail message
     */
    public ExpressionException(String string) {
        super(CoreException.EXPRESSION_ERROR, string);
    }

    /**
     * Creates an exception wrapping the given cause, reusing its message.
     *
     * @param e the underlying cause
     */
    public ExpressionException(Throwable e) {
        super(CoreException.EXPRESSION_ERROR, e.getMessage(), e);
    }

}
