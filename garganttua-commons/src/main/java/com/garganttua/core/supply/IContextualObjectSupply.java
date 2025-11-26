package com.garganttua.core.supply;

import java.util.Optional;

@FunctionalInterface
public interface IContextualObjectSupply<Supplied, Context> {

    Optional<Supplied> supplyObject(Context context, Object... otherContexts);

}
