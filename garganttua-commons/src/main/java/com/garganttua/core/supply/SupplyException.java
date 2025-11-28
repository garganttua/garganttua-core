package com.garganttua.core.supply;

import com.garganttua.core.CoreException;

public class SupplyException extends CoreException {

    public SupplyException(Exception e) {
        super(CoreException.SUPPLY_ERROR, e);
    }

    public SupplyException(String message) {
        super(CoreException.SUPPLY_ERROR, message);
    }

}
