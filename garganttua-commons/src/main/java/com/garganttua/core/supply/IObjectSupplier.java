package com.garganttua.core.supply;

import java.util.Optional;

public interface IObjectSupplier<Supplied> {

    Optional<Supplied> supply() throws SupplyException;

    Class<Supplied> getSuppliedType();

}
