package com.garganttua.core.supply;

import java.util.Optional;

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
