package com.garganttua.core.supply;

import com.garganttua.core.CoreException;
import com.garganttua.core.CoreExceptionCode;

public class SupplyException extends CoreException {

    public SupplyException(Exception e) {
        super(CoreExceptionCode.SUPPLY_ERROR, e);
    }

    public SupplyException(String message) {
        super(CoreExceptionCode.SUPPLY_ERROR, message);
    }

}
