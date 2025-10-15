package com.garganttua.injection.spec.supplier;

import java.util.Optional;

import com.garganttua.injection.DiException;

public interface IObjectSupplier<Supplied> {

    Optional<Supplied> getObject() throws DiException;

    Class<Supplied> getObjectClass();

}
