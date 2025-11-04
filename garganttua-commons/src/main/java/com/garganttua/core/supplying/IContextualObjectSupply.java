package com.garganttua.core.supplying;

import java.util.Optional;

@FunctionalInterface
public interface IContextualObjectSupply<Supplied, Context> {

    Optional<Supplied> supplyObject(Context context, Object... otherContexts);

}
