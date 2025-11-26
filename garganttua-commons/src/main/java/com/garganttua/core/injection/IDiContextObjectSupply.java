package com.garganttua.core.injection;

import com.garganttua.core.supply.IContextualObjectSupply;

@FunctionalInterface
public interface IDiContextObjectSupply<Supplied> extends IContextualObjectSupply<Supplied, IDiContext> {

}
