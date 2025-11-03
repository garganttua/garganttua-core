package com.garganttua.core.supplying;

import java.util.Optional;

public interface IObjectSupplier<Supplied> {

    Optional<Supplied> getObject() throws SupplyException;

    Class<Supplied> getObjectClass();

}
