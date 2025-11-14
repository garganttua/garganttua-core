package com.garganttua.core.condition;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class ConditionException extends CoreException {

    public ConditionException(String message) {
        super(CoreExceptionCode.CONDITION_ERROR, message);
    }

}
