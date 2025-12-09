package com.garganttua.core.expression;

import com.garganttua.core.CoreException;

public class ExpressionException extends CoreException {

    protected ExpressionException(String message, Throwable exception) {
        super(CoreException.EXPRESSION_ERROR, message, exception);
    }

    public ExpressionException(String string) {
        super(CoreException.EXPRESSION_ERROR, string);
    }

}
