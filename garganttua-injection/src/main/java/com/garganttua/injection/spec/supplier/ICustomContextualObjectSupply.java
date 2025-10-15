package com.garganttua.injection.spec.supplier;

import java.util.Optional;

@FunctionalInterface
public interface ICustomContextualObjectSupply<Supplied, Context> {

    Optional<Supplied> supplyObject(Context context);

}
