package com.garganttua.core.supplying;

import java.util.Optional;

public interface IObjectSupplier<Supplied> {

    Optional<Supplied> supply() throws SupplyException;

    Class<Supplied> getSuppliedType();

}
