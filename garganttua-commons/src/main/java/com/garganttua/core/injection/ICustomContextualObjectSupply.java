package com.garganttua.core.injection;

import java.util.Optional;

@FunctionalInterface
public interface ICustomContextualObjectSupply<Supplied, Context> {

    Optional<Supplied> supplyObject(Context context);

}
