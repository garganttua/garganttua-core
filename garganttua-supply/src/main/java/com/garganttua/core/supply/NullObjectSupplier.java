package com.garganttua.core.supply;

import java.util.Optional;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class NullObjectSupplier<SuppliedType> implements IObjectSupplier<SuppliedType>{

    private Class<SuppliedType> suppliedType;

    @Override
    public Optional<SuppliedType> supply() throws SupplyException {
        log.atTrace().log("Entering supply method");
        log.atDebug().log("Supplying null object for type {}", this.suppliedType.getSimpleName());
        log.atInfo().log("Supply completed for null object of type {}", this.suppliedType.getSimpleName());
        log.atTrace().log("Exiting supply method with empty result");
        return Optional.empty();
    }

    @Override
    public Class<SuppliedType> getSuppliedType() {
        return this.suppliedType;
    }

}
