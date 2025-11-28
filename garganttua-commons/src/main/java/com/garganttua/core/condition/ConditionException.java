package com.garganttua.core.condition;

import com.garganttua.core.CoreException;

public class ConditionException extends CoreException {

    public ConditionException(String message) {
        super(CoreException.CONDITION_ERROR, message);
    }

}
