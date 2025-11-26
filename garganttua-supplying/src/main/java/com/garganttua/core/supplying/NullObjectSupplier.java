package com.garganttua.core.supplying;

import java.util.Optional;

import com.garganttua.core.supply.IObjectSupplier;
import com.garganttua.core.supply.SupplyException;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NullObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType>{

    private Class<SuppliedType> suppliedType;

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        return Optional.empty();
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.suppliedType;
    }

}
